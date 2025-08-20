package com.demo.ec.repo;

import com.demo.ec.model.Category;
import com.demo.ec.model.Product;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DemoData {
    public static final List<Category> categories = List.of(
            new Category(1L, "家電"),
            new Category(2L, "ファッション"),
            new Category(3L, "食品")
    );

    public static final List<Product> products = new ArrayList<>(List.of(
            new Product(1L, 1L, "ヘッドフォン", "高音質ワイヤレスヘッドフォン", "https://picsum.photos/seed/headphone/400/300", new BigDecimal("9800")),
            new Product(2L, 1L, "キーボード", "メカニカルキーボード", "https://picsum.photos/seed/keyboard/400/300", new BigDecimal("5980")),
            new Product(3L, 2L, "Tシャツ", "コットン100%", "https://picsum.photos/seed/tshirt/400/300", new BigDecimal("1500")),
            new Product(4L, 3L, "コーヒー豆", "スペシャルティ 200g", "https://picsum.photos/seed/coffee/400/300", new BigDecimal("1200"))
    ));

    public static final Map<String, Object> orders = new ConcurrentHashMap<>();
}
