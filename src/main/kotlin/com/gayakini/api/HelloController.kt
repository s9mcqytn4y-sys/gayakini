package com.gayakini.api

import com.gayakini.common.api.StandardResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/hello", "/v1/hello")
@Tag(name = "Hello", description = "Endpoint penanda sistem aktif")
class HelloController {
    @GetMapping
    @Operation(summary = "Cek status layanan")
    fun hello(): StandardResponse<String> {
        return StandardResponse(data = "Halo, layanan gayakini API sudah aktif.", message = "Pesan sambutan")
    }
}
