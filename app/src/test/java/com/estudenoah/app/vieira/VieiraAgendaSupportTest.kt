package com.estudenoah.app.vieira

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VieiraAgendaSupportTest {
    @Test
    fun `accepts only https ASAV portal hosts`() {
        assertTrue(VieiraAgendaSupport.isAllowedPortalUrl("https://portal.asav.org.br/framehtml/web/app/edu/PortalEducacional/login/"))
        assertTrue(VieiraAgendaSupport.isAllowedPortalUrl("https://aluno.asav.org.br/alguma-pagina"))
        assertFalse(VieiraAgendaSupport.isAllowedPortalUrl("http://portal.asav.org.br/"))
        assertFalse(VieiraAgendaSupport.isAllowedPortalUrl("https://asav.org.br.evil.example/"))
        assertFalse(VieiraAgendaSupport.isAllowedPortalUrl("https://example.com/"))
    }

    @Test
    fun `cleans captured page text without destroying paragraphs`() {
        val raw = "  História   do Brasil  \r\n\r\n\r\n  Conteúdo realizado:\tBrasil Colônia  "
        assertEquals(
            "História do Brasil\n\nConteúdo realizado: Brasil Colônia",
            VieiraAgendaSupport.cleanCapturedText(raw)
        )
    }
}
