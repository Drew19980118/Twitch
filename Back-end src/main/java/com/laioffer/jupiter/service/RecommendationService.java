package com.laioffer.jupiter.service;

import com.laioffer.jupiter.dao.FavoriteDao;
import com.laioffer.jupiter.entity.db.Item;
import com.laioffer.jupiter.entity.db.ItemType;
import com.laioffer.jupiter.entity.response.Game;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class RecommendationService {

    private static final int DEFAULT_GAME_LIMIT = 3;
    private static final int DEFAULT_PER_GAME_RECOMMENDATION_LIMIT = 10;
    private static final int DEFAULT_TOTAL_RECOMMENDATION_LIMIT = 20;

    @Autowired
    private GameService gameService;

    @Autowired
    private FavoriteDao favoriteDao;

    public Map<String, List<Item>> recommendItemsByDefault() throws RecommendationException {
        Map<String, List<Item>> recommendedItemMap = new HashMap<>();
        List<Game> topGames;
        try {
            topGames = gameService.topGames(DEFAULT_GAME_LIMIT);
        } catch (TwitchException e) {
            throw new RecommendationException("Failed to get game data for recommendation");
        }
        for (ItemType type : ItemType.values()) {
            recommendedItemMap.put(type.toString(),recommendByTopGames(type, topGames));
        }
        return recommendedItemMap;
    }

    private List<Item> recommendByTopGames(ItemType type, List<Game> topGames) throws RecommendationException {
        List<Item> recommendedItems = new ArrayList<>();
        for (Game game : topGames) {
            List<Item> items;
            try {
                items = gameService.searchByType(game.getId(),type,DEFAULT_PER_GAME_RECOMMENDATION_LIMIT);
            } catch (TwitchException e) {
                throw new RecommendationException("Failed to get recommendation result");
            }
            for (Item item : items) {
                if (recommendedItems.size() == DEFAULT_TOTAL_RECOMMENDATION_LIMIT) {
                    return recommendedItems;
                }
                recommendedItems.add(item);
            } }
        return recommendedItems;
    }

    public Map<String, List<Item>> recommendItemsByUser(String userId) throws RecommendationException{
        Map<String, List<Item>> recommendedItemMap = new HashMap<>();
        Set<String> favoriteItemIds;
        Map<String, List<String>> favoriteGameIds;
        favoriteItemIds = favoriteDao.getFavoriteItemIds(userId);
        favoriteGameIds = favoriteDao.getFavoriteGameIds(favoriteItemIds);
        for (Map.Entry<String, List<String>> entry : favoriteGameIds.entrySet()) {
            if (entry.getValue().size() == 0) {
                List<Game> topGames;
                try {
                    topGames = gameService.topGames(DEFAULT_GAME_LIMIT);
                } catch (TwitchException e) {
                    throw new RecommendationException("Failed to get game data for recommendation");
                }
                recommendedItemMap.put(entry.getKey(),recommendByTopGames(ItemType.valueOf(entry.getKey()), topGames));
            } else {
                recommendedItemMap.put(entry.getKey(),recommendByFavoriteHistory(favoriteItemIds, entry.getValue(),ItemType.valueOf(entry.getKey())));
            }
        }
        return recommendedItemMap;
    }

    private List<Item> recommendByFavoriteHistory(Set<String> favoritedItemIds, List<String> favoriteGameIds, ItemType type) throws RecommendationException {
        Map<String, Long> favoriteGameIdByCount = new HashMap<>();
        for(String gameId : favoriteGameIds) {
            favoriteGameIdByCount.put(gameId, favoriteGameIdByCount.getOrDefault(gameId, 0L) + 1);
        }
        List<Map.Entry<String, Long>> sortedFavoriteGameIdListByCount = new ArrayList<>(favoriteGameIdByCount.entrySet());
        sortedFavoriteGameIdListByCount.sort((Map.Entry<String, Long> e1, Map.Entry<String, Long> e2) -> Long
                        .compare(e2.getValue(), e1.getValue()));
        if (sortedFavoriteGameIdListByCount.size() > DEFAULT_GAME_LIMIT) {
            sortedFavoriteGameIdListByCount = sortedFavoriteGameIdListByCount.subList(0, DEFAULT_GAME_LIMIT);
        }
        List<Item> recommendedItems = new ArrayList<>();
        for (Map.Entry<String, Long> favoriteGame : sortedFavoriteGameIdListByCount) {
            List<Item> items;
            try {
                items = gameService.searchByType(favoriteGame.getKey(),type, DEFAULT_PER_GAME_RECOMMENDATION_LIMIT);
            } catch (TwitchException e) {
                throw new RecommendationException("Failed to get recommendation result");
            }
            for (Item item : items) {
                if (recommendedItems.size() == DEFAULT_TOTAL_RECOMMENDATION_LIMIT) {
                    return recommendedItems;
                }
                if (!favoritedItemIds.contains(item.getId())) {
                    recommendedItems.add(item);
                }
            }
        }
        return recommendedItems;
    }


}
