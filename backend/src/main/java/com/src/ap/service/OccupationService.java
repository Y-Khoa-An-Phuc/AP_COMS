package com.src.ap.service;

import com.src.ap.dto.common.PageResponse;
import com.src.ap.audit.AuditSessionContextService;
import com.src.ap.dto.occupation.OccupationHistoryResponse;
import com.src.ap.dto.occupation.OccupationRequest;
import com.src.ap.dto.occupation.OccupationResponse;
import com.src.ap.entity.Occupation;
import com.src.ap.exception.BadRequestException;
import com.src.ap.exception.DuplicateResourceException;
import com.src.ap.exception.ResourceNotFoundException;
import com.src.ap.mapper.OccupationMapper;
import com.src.ap.repository.OccupationHistoryRepository;
import com.src.ap.repository.OccupationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class OccupationService {

    private final OccupationRepository occupationRepository;
    private final OccupationHistoryRepository occupationHistoryRepository;
    private final OccupationMapper occupationMapper;
    private final AuditSessionContextService auditSessionContextService;

    @Transactional(readOnly = true)
    public PageResponse<OccupationResponse> getAllOccupations(int page, int size, String sortBy, String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<Occupation> occupations = occupationRepository.findAll(pageable);
        Page<OccupationResponse> responsePage = occupations.map(occupationMapper::toResponse);
        return PageResponse.of(responsePage);
    }

    @Transactional(readOnly = true)
    public List<OccupationResponse> getAllOccupationsList() {
        return occupationRepository.findAll().stream()
                .map(occupationMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public OccupationResponse getOccupationById(Long id) {
        Occupation occupation = occupationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Occupation", "id", id));
        return occupationMapper.toResponse(occupation);
    }

    @Transactional(readOnly = true)
    public PageResponse<OccupationResponse> searchOccupations(String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Occupation> occupations = occupationRepository.searchByKeyword(keyword, pageable);
        Page<OccupationResponse> responsePage = occupations.map(occupationMapper::toResponse);
        return PageResponse.of(responsePage);
    }

    @Transactional
    public OccupationResponse createOccupation(OccupationRequest request) {
        if (occupationRepository.existsByName(request.getName())) {
            throw new DuplicateResourceException("Occupation", "name", request.getName());
        }

        auditSessionContextService.applySessionContextForWrite();
        Occupation occupation = occupationMapper.toEntity(request);
        occupation = occupationRepository.save(occupation);
        return occupationMapper.toResponse(occupation);
    }

    @Transactional
    public OccupationResponse updateOccupation(Long id, OccupationRequest request) {
        Occupation occupation = occupationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Occupation", "id", id));

        if (!occupation.getName().equals(request.getName()) && occupationRepository.existsByName(request.getName())) {
            throw new DuplicateResourceException("Occupation", "name", request.getName());
        }

        auditSessionContextService.applySessionContextForWrite();
        occupationMapper.updateEntity(request, occupation);
        occupation = occupationRepository.save(occupation);
        return occupationMapper.toResponse(occupation);
    }

    @Transactional
    public void deleteOccupation(Long id) {
        Occupation occupation = occupationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Occupation", "id", id));

        if (occupation.getEmployees() != null && !occupation.getEmployees().isEmpty()) {
            throw new BadRequestException("Cannot delete occupation with assigned employees");
        }

        auditSessionContextService.applySessionContextForWrite();
        occupationRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<OccupationHistoryResponse> getOccupationHistory(Long id) {
        if (!occupationRepository.existsById(id)) {
            throw new ResourceNotFoundException("Occupation", "id", id);
        }
        return occupationHistoryRepository.findByOccupationId(id);
    }

    @Transactional(readOnly = true)
    public Map<Long, List<OccupationHistoryResponse>> getOccupationHistoryByIds(List<Long> occupationIds) {
        return occupationHistoryRepository.findByOccupationIds(occupationIds);
    }
}
