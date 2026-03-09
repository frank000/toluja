package com.toluja.app.item;

import com.toluja.app.dto.ItemDtos;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

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
    private final ItemRepository itemRepository;

    public List<ItemDtos.SegmentResponse> listar(String tenantId) {
        return segmentRepository.findByTenantIdOrderByOrdemAscIdAsc(tenantId)
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
        segment.setOrdem(segmentRepository.findMaxOrdemByTenantId(tenantId) + 1);
        Segment saved = segmentRepository.save(segment);
        return new ItemDtos.SegmentResponse(saved.getId(), saved.getNome(), saved.getCor(), saved.getIcone());
    }

    @Transactional
    public void reordenar(ItemDtos.SegmentOrderRequest request, String tenantId) {
        List<Integer> segmentoIds = request.segmentoIds();
        if (segmentoIds == null || segmentoIds.isEmpty()) {
            throw new ResponseStatusException(BAD_REQUEST, "Lista de segmentos não pode ser vazia");
        }
        if (new HashSet<>(segmentoIds).size() != segmentoIds.size()) {
            throw new ResponseStatusException(BAD_REQUEST, "Lista de segmentos contém duplicidades");
        }

        List<Segment> segmentosAtuais = segmentRepository.findByTenantIdOrderByOrdemAscIdAsc(tenantId);
        if (segmentosAtuais.size() != segmentoIds.size()) {
            throw new ResponseStatusException(BAD_REQUEST, "A lista enviada deve conter todos os segmentos");
        }

        Set<Integer> idsAtuais = segmentosAtuais.stream().map(Segment::getId).collect(java.util.stream.Collectors.toSet());
        if (!idsAtuais.containsAll(segmentoIds)) {
            throw new ResponseStatusException(BAD_REQUEST, "A lista enviada contém segmentos inválidos");
        }

        Map<Integer, Segment> segmentosPorId = new HashMap<>();
        segmentosAtuais.forEach(segmento -> segmentosPorId.put(segmento.getId(), segmento));

        List<Segment> segmentosParaSalvar = new ArrayList<>(segmentosAtuais.size());
        for (int i = 0; i < segmentoIds.size(); i++) {
            Integer segmentoId = segmentoIds.get(i);
            Segment segmento = segmentosPorId.get(segmentoId);
            if (segmento == null) {
                throw new ResponseStatusException(BAD_REQUEST, "Segmento inválido");
            }
            segmentosParaSalvar.add(segmento);
        }
        aplicarOrdem(segmentosParaSalvar);
    }

    @Transactional
    public void excluir(Integer segmentoId, String tenantId) {
        Segment segmento = segmentRepository.findByIdAndTenantId(segmentoId, tenantId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Segmento não encontrado"));

        List<Item> itensComSegmento = itemRepository.findByAtivoTrueAndTenantIdAndSegment_Id(tenantId, segmentoId);
        if (!itensComSegmento.isEmpty()) {
            itensComSegmento.forEach(item -> item.setSegment(null));
            itemRepository.saveAll(itensComSegmento);
        }

        segmentRepository.delete(segmento);
        segmentRepository.flush();
        reindexarOrdem(tenantId);
    }

    private void reindexarOrdem(String tenantId) {
        List<Segment> segmentos = segmentRepository.findByTenantIdOrderByOrdemAscIdAsc(tenantId);
        aplicarOrdem(segmentos);
    }

    private void aplicarOrdem(List<Segment> segmentosNaOrdem) {
        if (segmentosNaOrdem.isEmpty()) {
            return;
        }
        int ordemTemporariaBase = -100000;
        for (int i = 0; i < segmentosNaOrdem.size(); i++) {
            segmentosNaOrdem.get(i).setOrdem(ordemTemporariaBase + i);
        }
        segmentRepository.saveAll(segmentosNaOrdem);
        segmentRepository.flush();

        for (int i = 0; i < segmentosNaOrdem.size(); i++) {
            segmentosNaOrdem.get(i).setOrdem(i + 1);
        }
        segmentRepository.saveAll(segmentosNaOrdem);
    }
}
