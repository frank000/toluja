package com.toluja.app.item;

import com.toluja.app.dto.ItemDtos;
import com.toluja.app.security.AuthContext;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/segmentos")
@RequiredArgsConstructor
public class SegmentController {

    private final SegmentService segmentService;

    @GetMapping
    public List<ItemDtos.SegmentResponse> listar(Authentication authentication) {
        return segmentService.listar(AuthContext.tenantId(authentication));
    }

    @PostMapping
    public ItemDtos.SegmentResponse criar(@Valid @RequestBody ItemDtos.SegmentRequest request, Authentication authentication) {
        return segmentService.criar(request, AuthContext.tenantId(authentication));
    }

    @PutMapping("/ordem")
    public void reordenar(@Valid @RequestBody ItemDtos.SegmentOrderRequest request, Authentication authentication) {
        segmentService.reordenar(request, AuthContext.tenantId(authentication));
    }

    @DeleteMapping("/{segmentoId}")
    public void excluir(@PathVariable Integer segmentoId, Authentication authentication) {
        segmentService.excluir(segmentoId, AuthContext.tenantId(authentication));
    }
}
