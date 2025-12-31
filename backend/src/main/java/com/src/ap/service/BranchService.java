package com.src.ap.service;

import com.src.ap.dto.branch.BranchRequest;
import com.src.ap.dto.branch.BranchResponse;
import com.src.ap.dto.common.PageResponse;
import com.src.ap.entity.Branch;
import com.src.ap.exception.BadRequestException;
import com.src.ap.exception.DuplicateResourceException;
import com.src.ap.exception.ResourceNotFoundException;
import com.src.ap.mapper.BranchMapper;
import com.src.ap.repository.BranchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BranchService {

    private final BranchRepository branchRepository;
    private final BranchMapper branchMapper;

    @Transactional(readOnly = true)
    public PageResponse<BranchResponse> getAllBranches(int page, int size, String sortBy, String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<Branch> branches = branchRepository.findAll(pageable);
        Page<BranchResponse> responsePage = branches.map(branchMapper::toResponse);
        return PageResponse.of(responsePage);
    }

    @Transactional(readOnly = true)
    public List<BranchResponse> getAllBranchesList() {
        return branchRepository.findAll().stream()
                .map(branchMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public BranchResponse getBranchById(Long id) {
        Branch branch = branchRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Branch", "id", id));
        return branchMapper.toResponse(branch);
    }

    @Transactional(readOnly = true)
    public PageResponse<BranchResponse> searchBranches(String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Branch> branches = branchRepository.searchByKeyword(keyword, pageable);
        Page<BranchResponse> responsePage = branches.map(branchMapper::toResponse);
        return PageResponse.of(responsePage);
    }

    @Transactional
    public BranchResponse createBranch(BranchRequest request) {
        if (branchRepository.existsByName(request.getName())) {
            throw new DuplicateResourceException("Branch", "name", request.getName());
        }

        Branch branch = branchMapper.toEntity(request);
        branch = branchRepository.save(branch);
        return branchMapper.toResponse(branch);
    }

    @Transactional
    public BranchResponse updateBranch(Long id, BranchRequest request) {
        Branch branch = branchRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Branch", "id", id));

        if (!branch.getName().equals(request.getName()) && branchRepository.existsByName(request.getName())) {
            throw new DuplicateResourceException("Branch", "name", request.getName());
        }

        branchMapper.updateEntity(request, branch);
        branch = branchRepository.save(branch);
        return branchMapper.toResponse(branch);
    }

    @Transactional
    public void deleteBranch(Long id) {
        Branch branch = branchRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Branch", "id", id));

        if (branch.getEmployees() != null && !branch.getEmployees().isEmpty()) {
            throw new BadRequestException("Cannot delete branch with assigned employees");
        }

        branchRepository.deleteById(id);
    }
}