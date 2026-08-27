package com.estudenoah.app.youtube

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class YoutubePlaybackLauncherTest {
    @Test fun acceptsOfficialHttpsYoutubeUrls() {
        assertEquals("https://www.youtube.com/watch?v=AbCdEf123_-", YoutubePlaybackLauncher.supportedUrl(" https://www.youtube.com/watch?v=AbCdEf123_- "))
        assertEquals("https://youtube.com/watch?v=AbCdEf123_-", YoutubePlaybackLauncher.supportedUrl("https://youtube.com/watch?v=AbCdEf123_-"))
        assertEquals("https://m.youtube.com/watch?v=AbCdEf123_-", YoutubePlaybackLauncher.supportedUrl("https://m.youtube.com/watch?v=AbCdEf123_-"))
        assertEquals("https://youtu.be/AbCdEf123_-", YoutubePlaybackLauncher.supportedUrl("https://youtu.be/AbCdEf123_-"))
    }

    @Test fun rejectsNonHttpsAndLookalikeDomains() {
        assertNull(YoutubePlaybackLauncher.supportedUrl("http://youtube.com/watch?v=AbCdEf123_-"))
        assertNull(YoutubePlaybackLauncher.supportedUrl("https://youtube.com.evil.example/watch?v=AbCdEf123_-"))
        assertNull(YoutubePlaybackLauncher.supportedUrl("https://example.com/youtube.com/watch?v=AbCdEf123_-"))
        assertNull(YoutubePlaybackLauncher.supportedUrl("javascript:alert(1)"))
    }

    @Test fun rejectsCredentialsAndBlankInput() {
        assertNull(YoutubePlaybackLauncher.supportedUrl("https://user:pass@youtube.com/watch?v=AbCdEf123_-"))
        assertNull(YoutubePlaybackLauncher.supportedUrl("   "))
    }
}
