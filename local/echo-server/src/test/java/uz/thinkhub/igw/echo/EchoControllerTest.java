package uz.thinkhub.igw.echo;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

@SpringBootTest(
        classes = EchoServerApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.MOCK
)
class EchoControllerTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @Test
    void echoesGetRequestWithPathAndHeaders() throws Exception {
        mockMvc.perform(get("/test/path")
                        .header("X-Custom-Header", "value-1"))
                .andExpect(result -> {
                    String body = result.getResponse().getContentAsString();
                    assertThat(body).contains("\"method\":\"GET\"");
                    assertThat(body).contains("\"path\":\"/test/path\"");
                    assertThat(body).contains("X-Custom-Header");
                    assertThat(body).contains("value-1");
                });
    }

    @Test
    void echoesArbitraryPath() throws Exception {
        mockMvc.perform(get("/some/random/path"))
                .andExpect(result -> {
                    String body = result.getResponse().getContentAsString();
                    assertThat(body).contains("\"path\":\"/some/random/path\"");
                });
    }
}
