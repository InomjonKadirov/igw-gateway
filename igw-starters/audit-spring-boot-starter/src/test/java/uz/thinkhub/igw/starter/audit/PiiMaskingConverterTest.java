package uz.thinkhub.igw.starter.audit;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PiiMaskingConverterTest {

    private final IgwAuditProperties props = new IgwAuditProperties();
    private final PiiMaskingConverter converter = new PiiMaskingConverter(props);

    @Test
    void masksPanField() {
        String input = "{\"pan\": \"4111111111111111\", \"amount\": 100}";
        String output = converter.maskJsonBody(input);

        assertThat(output).contains("\"pan\": \"***\"");
        assertThat(output).contains("\"amount\": 100");  // non-PII preserved
    }

    @Test
    void masksCardField() {
        String input = "{\"card\": \"1234-5678-9012-3456\"}";
        String output = converter.maskJsonBody(input);

        assertThat(output).contains("\"card\": \"***\"");
    }

    @Test
    void masksCardNumberCamelCase() {
        String input = "{\"cardNumber\": \"1234567890123456\"}";
        String output = converter.maskJsonBody(input);

        assertThat(output).contains("\"cardNumber\": \"***\"");
    }

    @Test
    void masksCvv() {
        String input = "{\"cvv\": \"123\", \"pan\": \"4111111111111111\"}";
        String output = converter.maskJsonBody(input);

        assertThat(output).contains("\"cvv\": \"***\"");
        assertThat(output).contains("\"pan\": \"***\"");
    }

    @Test
    void masksContactFields() {
        String input = "{\"passport\": \"AB1234567\", \"pin\": \"1234\", \"phone\": \"+998901234567\", \"email\": \"a@b.com\"}";
        String output = converter.maskJsonBody(input);

        assertThat(output).contains("\"passport\": \"***\"");
        assertThat(output).contains("\"pin\": \"***\"");
        assertThat(output).contains("\"phone\": \"***\"");
        assertThat(output).contains("\"email\": \"***\"");
    }

    @Test
    void preservesNonPiiFields() {
        String input = "{\"userId\": \"u-1\", \"amount\": 100, \"currency\": \"USD\"}";
        String output = converter.maskJsonBody(input);

        assertThat(output).isEqualTo(input);
    }

    @Test
    void handlesWhitespaceInJson() {
        String input = "{ \"pan\"  :  \"4111\" }";
        String output = converter.maskJsonBody(input);

        assertThat(output).contains("\"pan\"  :  \"***\"");
    }

    @Test
    void handlesNullAndEmpty() {
        assertThat(converter.maskJsonBody(null)).isNull();
        assertThat(converter.maskJsonBody("")).isEqualTo("");
    }

    @Test
    void customMaskingFieldsAreRespected() {
        IgwAuditProperties custom = new IgwAuditProperties();
        custom.setMaskingFields(java.util.List.of("secret", "token"));
        PiiMaskingConverter customConverter = new PiiMaskingConverter(custom);

        String input = "{\"secret\": \"shh\", \"other\": \"value\", \"token\": \"abc\"}";
        String output = customConverter.maskJsonBody(input);

        assertThat(output).contains("\"secret\": \"***\"");
        assertThat(output).contains("\"other\": \"value\"");
        assertThat(output).contains("\"token\": \"***\"");
    }

    @Test
    void multipleOccurrencesOfSameFieldAreMasked() {
        String input = "{\"pan\": \"1111\", \"nested\": {\"pan\": \"2222\"}}";
        String output = converter.maskJsonBody(input);

        assertThat(output).doesNotContain("\"pan\": \"1111\"");
        assertThat(output).doesNotContain("\"pan\": \"2222\"");
    }
}
