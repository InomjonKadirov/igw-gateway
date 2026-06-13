package uz.thinkhub.igw.starter.errormap;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry of {@link ProviderErrorTranslator}s keyed by provider name.
 *
 * <p>The {@code X-Provider} request header is used to look up the matching
 * translator. Per-provider implementations are registered at startup, e.g.
 * {@code registry.register("iabs", new IabsErrorTranslator())}.
 *
 * <p>This class is thread-safe. Registering the same provider twice overrides
 * the previous translator.
 */
public class ProviderErrorTranslatorRegistry {

    private final Map<String, ProviderErrorTranslator> translators = new ConcurrentHashMap<>();

    public void register(String provider, ProviderErrorTranslator translator) {
        translators.put(provider, translator);
    }

    public Optional<ProviderErrorTranslator> get(String provider) {
        return Optional.ofNullable(translators.get(provider));
    }

    public int size() {
        return translators.size();
    }
}
