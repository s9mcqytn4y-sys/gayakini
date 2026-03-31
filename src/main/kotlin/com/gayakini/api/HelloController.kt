package com.gayakini.api

import com.gayakini.common.api.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/hello")
@Tag(name = "Hello", description = "Endpoint penanda sistem aktif")
class HelloController {

    @GetMapping
    @Operation(summary = "Cek status layanan")
    fun hello(): ApiResponse<String> {
        return ApiResponse.success("Halo, layanan gayakini API sudah aktif.", "Pesan sambutan")
    }
}
