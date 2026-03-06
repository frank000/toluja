package com.toluja.app.item;

import com.toluja.app.dto.ItemDtos;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@Service
@RequiredArgsConstructor
public class SegmentService {

    private static final Pattern HEX_COLOR = Pattern.compile("^#[0-9A-Fa-f]{6}$");
    private static final Set<String> ICONES_PERMITIDOS = Set.of(
            "bi-cup-straw",
            "bi-cup-hot",
            "bi-egg-fried",
            "bi-basket2",
            "bi-ice-cream",
            "bi-star",
            "bi-tag"
    );

    private final SegmentRepository segmentRepository;

    public List<ItemDtos.SegmentResponse> listar(String tenantId) {
        return segmentRepository.findByTenantIdOrderByNomeAsc(tenantId)
                .stream()
                .map(segment -> new ItemDtos.SegmentResponse(segment.getId(), segment.getNome(), segment.getCor(), segment.getIcone()))
                .toList();
    }

    public ItemDtos.SegmentResponse criar(ItemDtos.SegmentRequest request, String tenantId) {
        String nome = request.nome().trim();
        String cor = request.cor().trim();
        String icone = request.icone().trim();

        if (!HEX_COLOR.matcher(cor).matches()) {
            throw new ResponseStatusException(BAD_REQUEST, "Cor inválida. Use formato hexadecimal, ex.: #1A73E8");
        }
        if (!ICONES_PERMITIDOS.contains(icone)) {
            throw new ResponseStatusException(BAD_REQUEST, "Ícone inválido");
        }
        if (segmentRepository.existsByTenantIdAndNomeIgnoreCase(tenantId, nome)) {
            throw new ResponseStatusException(BAD_REQUEST, "Já existe segmento com esse nome");
        }

        Segment segment = new Segment();
        segment.setNome(nome);
        segment.setCor(cor);
        segment.setIcone(icone);
        segment.setTenantId(tenantId);
        Segment saved = segmentRepository.save(segment);
        return new ItemDtos.SegmentResponse(saved.getId(), saved.getNome(), saved.getCor(), saved.getIcone());
    }
}
