package no.nav.altinnkanal

import org.eclipse.jetty.server.Request
import org.eclipse.jetty.server.handler.AbstractHandler

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

const val APPLICATION_ALIVE = "Application is alive"
const val APPLICATION_READY = "Application is ready"

class SelfTestHandler : AbstractHandler() {

    override fun handle(target: String, baseRequest: Request, request: HttpServletRequest, response: HttpServletResponse) {
        response.contentType = ("text/html; charset=utf-8")
        response.status = HttpServletResponse.SC_OK
        when (target) {
            "/is_alive" -> {
                response.writer.println(APPLICATION_ALIVE)
                baseRequest.isHandled = true
            }
            "/is_ready" -> {
                response.writer.println(APPLICATION_READY)
                baseRequest.isHandled = true
            }
        }
    }
}
