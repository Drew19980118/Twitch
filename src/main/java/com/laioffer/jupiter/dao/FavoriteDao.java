package com.laioffer.jupiter.dao;

import com.laioffer.jupiter.entity.db.Item;
import com.laioffer.jupiter.entity.db.ItemType;
import com.laioffer.jupiter.entity.db.User;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
public class FavoriteDao {

    @Autowired
    private SessionFactory sessionFactory;

    public void setFavoriteItem(String userId, Item item){
        Session session = null;
        try {
            session = sessionFactory.openSession();
            User user = session.get(User.class, userId);
            user.getItemSet().add(item);
            session.beginTransaction();
            session.update(user);
            session.getTransaction().commit();
        }catch (Exception ex){
            ex.printStackTrace();
            session.getTransaction().rollback();
        } finally {
            if (session != null) session.close();
        }
    }

    public void unsetFavoriteItem(String userId, String itemId){
        Session session = null;
        try {
            session = sessionFactory.openSession();
            User user = session.get(User.class, userId);
            Item item = session.get(Item.class, itemId);
            user.getItemSet().remove(item);
            session.beginTransaction();
            session.update(user);
            session.getTransaction().commit();
        }catch (Exception ex){
            ex.printStackTrace();
            session.getTransaction().rollback();
        }finally {
            if (session != null) session.close();
        }
    }

    public Set<Item> getFavoriteItems(String userId){
        Session session = null;
        try {
            session = sessionFactory.openSession();
            return session.get(User.class, userId).getItemSet();
        }catch (Exception ex){
            ex.printStackTrace();
        }finally {
            if (session != null) session.close();
        }
        return new HashSet<>();
    }

    public Set<String> getFavoriteItemIds(String userId){
        Set<String> itemIds = new HashSet<>();
        Session session = null;
        try {
            session = sessionFactory.openSession();
            User user = session.get(User.class, userId);
            if (user == null) return itemIds;
            Set<Item> items = user.getItemSet();
            for(Item item : items) {
                itemIds.add(item.getId());
            }
        }catch (Exception ex) {
            ex.printStackTrace();
        }finally {
            if (session != null) session.close();
        }
        return itemIds;
    }

    public Map<String, List<String>> getFavoriteGameIds(Set<String> favoriteItemIds) {
        Map<String, List<String>> itemMap = new HashMap<>();
        for (ItemType type : ItemType.values()) {
            itemMap.put(type.toString(), new ArrayList<>());
        }
        Session session = null;
        try{
            session = sessionFactory.openSession();
            for(String itemId : favoriteItemIds) {
                Item item = session.get(Item.class, itemId);
                itemMap.get(item.getType().toString()).add(item.getGameId());
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }finally {
            if (session != null) session.close();
        }
        return itemMap;
    }
}
