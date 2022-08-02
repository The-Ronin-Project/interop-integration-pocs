package com.projectronin.interop.openapi.impl

import com.projectronin.interop.openapi.apis.ChannelsApi
import com.projectronin.interop.openapi.models.Channel
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController

@RestController
class ChannelController : ChannelsApi {
    override fun channelsGet(): ResponseEntity<Channel> {
        return ResponseEntity.ok().build()
    }
}
