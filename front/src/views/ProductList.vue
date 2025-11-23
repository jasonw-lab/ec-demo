<template>
  <div>
    <!-- メルカリ風のプロモーションバナー -->
    <div style="margin:24px 0;">
      <img :src="getImageUrl('/line.jpg')" alt="LINE友だち追加" style="width:100%;height:auto;border-radius:8px;" />
    </div>

    <!-- 掘り出し物セクション -->
    <div style="margin-bottom:24px;">
      <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:16px;">
        <h2 style="margin:0;font-size:16px;font-weight:600;">掘り出し物300円オークション</h2>
        <a @click="showUnderConstruction" style="color:#3b82f6;text-decoration:none;font-size:14px;cursor:pointer;">すべて見る ></a>
      </div>
      
      <!-- 商品グリッド -->
      <div style="display:grid;grid-template-columns:repeat(4,1fr);gap:16px;">
        <div v-for="(p, idx) in auctionProducts" :key="p.id" style="background:#fff;border-radius:8px;overflow:hidden;box-shadow:0 2px 8px rgba(0,0,0,0.1);display:flex;flex-direction:column;">
          <img :src="p.imageUrl || getImageUrl('/product.svg')" alt="商品画像" style="width:100%;height:240px;object-fit:cover;background:#f9fafb" />
          <div style="padding:12px;display:flex;flex-direction:column;flex:1;">
            <div style="font-size:14px;line-height:1.4;margin-bottom:8px;display:-webkit-box;-webkit-line-clamp:2;-webkit-box-orient:vertical;overflow:hidden;">
              {{ p.name }}
            </div>
            <div style="margin-top:auto;display:flex;justify-content:space-between;align-items:center;">
              <div style="color:#ff6b6b;font-weight:700;font-size:16px;">現在¥300</div>
              <button @click="add(p)" style="background:#ff6b6b;color:white;border:none;border-radius:6px;padding:6px 12px;cursor:pointer;font-size:12px;">カートに追加</button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 工事中メッセージモーダル -->
    <div v-if="showModal" style="position:fixed;top:0;left:0;width:100%;height:100%;background:rgba(0,0,0,0.5);display:flex;align-items:center;justify-content:center;z-index:1000;">
      <div style="background:#fff;border-radius:12px;padding:32px;text-align:center;max-width:400px;margin:16px;">
        <div style="font-size:48px;margin-bottom:16px;">🚧</div>
        <h3 style="margin:0 0 16px 0;font-size:20px;font-weight:600;color:#111827;">工事中</h3>
        <p style="margin:0 0 24px 0;color:#6b7280;line-height:1.5;">この機能は現在開発中です。<br>もうしばらくお待ちください。</p>
        <button @click="hideModal" style="background:#ff6b6b;color:#fff;border:none;border-radius:8px;padding:12px 24px;cursor:pointer;font-weight:600;font-size:16px;">
          閉じる
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useStore, type Product, getImageUrl } from '../store'

const store = useStore()
const showModal = ref<boolean>(false)

function add(product: Product): void { 
  store.addToCart(product, 1) 
}

function showUnderConstruction() {
  showModal.value = true
}

function hideModal() {
  showModal.value = false
}

// オークション商品（掘り出し物）
const auctionProducts = computed<Product[]>(() => {
  return [
    { id: 'auction-1', name: '40 Dickies ショートパンツ ハーフパンツ グレー XL相当', price: 300, imageUrl: getImageUrl('/product/40_dickies.jpg') } as Product,
    { id: 'auction-2', name: '36 Dickies ショートパンツ ハーフパンツ XL相当', price: 300, imageUrl: getImageUrl('/product/36_dickies.jpg') } as Product,
    { id: 'auction-3', name: '☆Paul Smith 紫色カーディガン☆', price: 300, imageUrl: getImageUrl('/product/Paul_Smith.jpg') } as Product,
    { id: 'auction-4', name: 'Carhartt ネイビー ワークパンツ メキシコ製 40×34 2XL相当', price: 300, imageUrl: getImageUrl('/product/Carhartt.jpg') } as Product,
    { id: 'auction-5', name: 'LooseFit 48 Dickies ショートパンツ ハーフパンツ 4XL相当', price: 300, imageUrl: getImageUrl('/product/LooseFit.jpg') } as Product,
    { id: 'auction-6', name: 'ビューティーアンドユース ユナイテッドアローズ チェックシャツ...', price: 300, imageUrl: getImageUrl('/product/Vanity_United.jpg') } as Product,
    { id: 'auction-7', name: 'ヴィンテージ90s メイドインアメリカusa カレッジ スウェットフーディ 古着..', price: 300, imageUrl: getImageUrl('/product/edwards.jpg') } as Product,
    { id: 'auction-8', name: 'NMB48 北川謙二 CD/DVD レンタル専用', price: 300, imageUrl: getImageUrl('/product/nmb48.jpg') } as Product
  ]
})
</script>
