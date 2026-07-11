package com.paw.ddasoom.animal.dto.response;

import com.paw.ddasoom.animal.domain.Animal;

public record AnimalResponse(
        Long id,
        String abandonmentId,
        String kind,
        String nickname,
        String gender,
        String typeName,
        String age,
        String imageUrl,
        int likeCount,
        boolean isFostered
) {
    public static AnimalResponse from(Animal animal) {
        return new AnimalResponse(
                animal.getId(),
                animal.getAbandonmentId(),
                animal.getKind().name(),
                animal.getNickname(),
                animal.getGender().name(),
                animal.getTypeName(),
                animal.getAge(),
                animal.getImageUrl(),
                animal.getLikeCount(),
                animal.isFostered()
        );
    }
}