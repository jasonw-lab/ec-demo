<template>
  <div>
    <!-- メルカリ風のプロモーションバナー -->
    <div style="margin:24px 0;">
      <img src="/line.jpg" alt="LINE友だち追加" style="width:100%;height:auto;border-radius:8px;" />
    </div>

    <!-- 掘り出し物セクション -->
    <div style="margin-bottom:24px;">
              <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:16px;">
          <h2 style="margin:0;font-size:20px;font-weight:600;">掘り出し物300円オークション</h2>
          <router-link to="/products" style="color:#3b82f6;text-decoration:none;font-size:14px;">すべて見る ></router-link>
        </div>
      
      <!-- 商品グリッド -->
      <div style="display:grid;grid-template-columns:repeat(4,1fr);gap:16px;">
        <div v-for="(p, idx) in auctionProducts" :key="p.id" style="background:#fff;border-radius:8px;overflow:hidden;box-shadow:0 2px 8px rgba(0,0,0,0.1);">
          <img :src="p.imageUrl || '/product.svg'" alt="商品画像" style="width:100%;height:240px;object-fit:cover;background:#f9fafb" />
          <div style="padding:12px;">
            <div style="font-size:14px;line-height:1.4;margin-bottom:8px;display:-webkit-box;-webkit-line-clamp:2;-webkit-box-orient:vertical;overflow:hidden;">
              {{ p.name }}
            </div>
            <div style="display:flex;justify-content:space-between;align-items:center;">
              <div style="color:#ff6b6b;font-weight:700;font-size:16px;">現在¥300</div>
              <button @click="add(p)" style="background:#ff6b6b;color:white;border:none;border-radius:6px;padding:8px;cursor:pointer;font-size:14px;">カートに追加</button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 商品一覧へのリンク -->
    <!-- <div style="text-align:center;margin:32px 0;">
      <router-link to="/products" style="display:inline-block;background:#ff6b6b;color:white;border:none;border-radius:8px;padding:16px 32px;text-decoration:none;font-weight:600;font-size:18px;">
        すべての商品を見る
      </router-link>
    </div> -->

    <!-- 人気のブランドセクション -->
    <!-- <div style="margin-top:32px;">
 
  </div>     <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:16px;">
        <h2 style="margin:0;font-size:20px;font-weight:600;">人気のブランド</h2>
        <router-link to="/products" style="color:#3b82f6;text-decoration:none;font-size:14px;">すべて見る ></router-link>
      </div>
      <div style="background:#fff;border-radius:8px;padding:24px;text-align:center;color:#6b7280;">
        ブランド情報を読み込み中...
      </div>　-->
    </div> 
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useStore, type Product } from '../store'

const store = useStore()

function add(product: Product): void { store.addToCart(product, 1) }

// オークション商品（掘り出し物）
const auctionProducts = computed<Product[]>(() => {
  return [
    { id: 'auction-1', name: '40 Dickies ショートパンツ ハーフパンツ グレー XL相当', price: 300, imageUrl: '/product/40_dickies.jpg' } as Product,
    { id: 'auction-2', name: '36 Dickies ショートパンツ ハーフパンツ XL相当', price: 300, imageUrl: '/product/36_dickies.jpg' } as Product,
    { id: 'auction-3', name: '☆Paul Smith 紫色カーディガン☆', price: 300, imageUrl: '/product/Paul_Smith.jpg' } as Product,
    { id: 'auction-4', name: 'Carhartt ネイビー ワークパンツ メキシコ製 40×34 2XL相当', price: 300, imageUrl: '/product/Carhartt.jpg' } as Product,
    { id: 'auction-5', name: 'LooseFit 48 Dickies ショートパンツ ハーフパンツ 4XL相当', price: 300, imageUrl: '/product/LooseFit.jpg' } as Product,
    { id: 'auction-6', name: 'ビューティーアンドユース ユナイテッドアローズ チェックシャツ...', price: 300, imageUrl: '/product/Vanity_United.jpg' } as Product,
    { id: 'auction-7', name: 'ヴィンテージ90s メイドインアメリカusa カレッジ スウェットフーディ 古着..', price: 300, imageUrl: '/product/edwards.jpg' } as Product,
    { id: 'auction-7', name: 'NMB48 北川謙二 CD/DVD レンタル専用', price: 300, imageUrl: '/product/nmb48.jpg' } as Product

  ]
})
</script>
