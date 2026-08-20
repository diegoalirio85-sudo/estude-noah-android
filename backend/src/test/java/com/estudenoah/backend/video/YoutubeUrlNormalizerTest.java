package com.estudenoah.backend.video;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.estudenoah.backend.api.ApiException;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class YoutubeUrlNormalizerTest {
    private final YoutubeUrlNormalizer normalizer = new YoutubeUrlNormalizer();

    @ParameterizedTest
    @MethodSource("officialUrls")
    void acceptsAndNormalizesOfficialPublicVideoUrls(String input) {
        assertThat(normalizer.normalize(input).toString())
                .isEqualTo("https://www.youtube.com/watch?v=AbCdEf123_- ".trim());
    }

    static Stream<String> officialUrls() {
        return Stream.of(
                "https://youtube.com/watch?v=AbCdEf123_-",
                "https://www.youtube.com/watch?v=AbCdEf123_-&t=10",
                "https://m.youtube.com/watch?v=AbCdEf123_-",
                "https://youtu.be/AbCdEf123_-"
        );
    }

    @ParameterizedTest
    @MethodSource("unsafeUrls")
    void rejectsNonOfficialOrUnsafeUrls(String input) {
        assertThatThrownBy(() -> normalizer.normalize(input))
                .isInstanceOf(ApiException.class)
                .extracting(error -> ((ApiException) error).code())
                .isEqualTo("invalid_youtube_url");
    }

    static Stream<Arguments> unsafeUrls() {
        return Stream.of(
                Arguments.of((String) null),
                Arguments.of(""),
                Arguments.of("http://www.youtube.com/watch?v=AbCdEf123_-"),
                Arguments.of("https://youtube.com.evil.example/watch?v=AbCdEf123_-"),
                Arguments.of("https://127.0.0.1/watch?v=AbCdEf123_-"),
                Arguments.of("file:///tmp/video"),
                Arguments.of("https://www.youtube.com:443/watch?v=AbCdEf123_-"),
                Arguments.of("https://www.youtube.com/watch?v=%ZZinvalid"),
                Arguments.of("https://www.youtube.com/watch?v=AbCdEf123_-#fragment"),
                Arguments.of("https://www.youtube.com/redirect?q=https://example.com"),
                Arguments.of("https://youtu.be/invalid")
        );
    }
}
