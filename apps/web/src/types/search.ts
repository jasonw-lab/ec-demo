import type { Product } from '../store'

/**
 * Elasticsearch検索APIのレスポンス形式
 * BFFは SearchResponse を直接返却する（CommonResponse でラップしない）
 */
export interface SearchApiResponse {
  items: ProductCard[]
  total: number
  page: number
  size: number
  didYouMean?: string
}

/**
 * Elasticsearch検索結果の商品カード
 * ES の ProductDocument に対応
 */
export interface ProductCard {
  productId: number
  title: string
  price: number
  thumbnailUrl: string
  createdAt: string
}

/**
 * ProductCard を既存の Product 型に変換する
 * フィールドマッピング:
 * - productId → id
 * - title → name
 * - thumbnailUrl → imageUrl
 * - price → price (そのまま)
 * - description: API に含まれないため空文字を設定
 */
export function productCardToProduct(card: ProductCard): Product {
  return {
    id: card.productId,
    name: card.title,
    description: '', // API に含まれない
    price: card.price,
    imageUrl: card.thumbnailUrl,
  }
}
