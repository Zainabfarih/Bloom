package com.bloom.jobservice.service;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class SkillMatchingService {

    public int computeScore(List<String> required, List<String> studentSkills) {
        if (required == null || required.isEmpty()) return 0;
        Set<String> normalized = normalizeAll(studentSkills);
        long matched = required.stream()
                .map(this::normalize)
                .filter(normalized::contains)
                .count();
        return (int) Math.round((double) matched / required.size() * 100);
    }

    public List<String> findMatched(List<String> required, List<String> studentSkills) {
        if (required == null) return List.of();
        Set<String> normalized = normalizeAll(studentSkills);
        return required.stream()
                .filter(s -> normalized.contains(normalize(s)))
                .toList();
    }

    public List<String> findMissing(List<String> required, List<String> studentSkills) {
        if (required == null) return List.of();
        Set<String> normalized = normalizeAll(studentSkills);
        return required.stream()
                .filter(s -> !normalized.contains(normalize(s)))
                .toList();
    }

    private String normalize(String skill) {
        return skill.toLowerCase().replaceAll("[^a-z0-9]", "");
    }

    private Set<String> normalizeAll(List<String> skills) {
        if (skills == null) return Set.of();
        return skills.stream()
                .map(this::normalize)
                .collect(Collectors.toSet());
    }
}