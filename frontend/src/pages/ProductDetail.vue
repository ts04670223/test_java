<template>
  <v-container class="py-4" style="max-width: 1200px">
    <!-- 載入中 -->
    <div v-if="loading" class="d-flex justify-center pa-8">
      <v-progress-circular indeterminate color="primary" size="64" />
    </div>

    <!-- 錯誤 -->
    <v-alert v-else-if="error" type="error" class="ma-4">找不到該產品或載入失敗</v-alert>

    <template v-else-if="product">
      <v-row>
        <!-- 商品圖片 -->
        <v-col cols="12" md="6">
          <v-card>
            <v-img
              :src="product.images?.[selectedImage]?.url || '/api/placeholder/600/400'"
              height="400"
              cover
            />
          </v-card>
          <div v-if="product.images && product.images.length > 1" class="d-flex ga-2 mt-2 overflow-auto">
            <v-card
              v-for="(img, idx) in product.images"
              :key="idx"
              :class="['flex-shrink-0', selectedImage === idx ? 'border-primary border-md' : '']"
              width="80"
              @click="selectedImage = idx"
              style="cursor: pointer"
            >
              <v-img :src="img.url" height="80" cover />
            </v-card>
          </div>
        </v-col>

        <!-- 商品資訊 -->
        <v-col cols="12" md="6">
          <h1 class="text-h4 font-weight-bold mb-2">{{ product.name }}</h1>

          <div class="d-flex align-center ga-2 mb-4">
            <v-rating
              :model-value="product.averageRating || 0"
              readonly
              half-increments
              density="compact"
              color="amber"
            />
            <span class="text-body-2 text-medium-emphasis">({{ product.reviewCount || 0 }} 評論)</span>
          </div>

          <p class="text-h5 text-primary font-weight-bold mb-3">NT$ {{ currentPrice }}</p>

          <p class="text-body-1 mb-4">{{ product.description }}</p>

          <!-- 規格選擇 -->
          <div v-if="product.variants && product.variants.length > 0" class="mb-4">
            <p class="text-subtitle-1 font-weight-medium mb-2">選擇規格</p>
            <div class="d-flex flex-wrap ga-2">
              <v-chip
                v-for="variant in product.variants"
                :key="variant.id"
                :color="selectedVariant?.id === variant.id ? 'primary' : undefined"
                :variant="selectedVariant?.id === variant.id ? 'elevated' : 'outlined'"
                @click="selectedVariant = variant"
              >
                {{ variant.name }} - NT$ {{ variant.price }}
              </v-chip>
            </div>
          </div>

          <!-- 標籤 -->
          <div v-if="product.tags && product.tags.length > 0" class="mb-4">
            <div class="d-flex flex-wrap ga-1">
              <v-chip v-for="tag in product.tags" :key="tag.id" size="small" variant="outlined">
                {{ tag.name }}
              </v-chip>
            </div>
          </div>

          <!-- 數量選擇 -->
          <div class="d-flex align-center ga-3 mb-4">
            <span class="text-subtitle-1 font-weight-medium">數量：</span>
            <div class="d-flex align-center">
              <v-btn icon size="small" variant="text" :disabled="quantity <= 1" @click="quantity--">
                <v-icon>mdi-minus</v-icon>
              </v-btn>
              <span class="mx-3 text-subtitle-1" style="min-width: 32px; text-align: center">{{ quantity }}</span>
              <v-btn icon size="small" variant="text" :disabled="quantity >= stockQty" @click="quantity++">
                <v-icon>mdi-plus</v-icon>
              </v-btn>
            </div>
            <span class="text-body-2 text-medium-emphasis">庫存：{{ stockQty }}</span>
          </div>

          <!-- 操作按鈕 -->
          <div class="d-flex ga-2">
            <v-btn
              color="primary"
              size="large"
              prepend-icon="mdi-cart"
              :disabled="stockQty === 0 || addingToCart"
              :loading="addingToCart"
              class="flex-grow-1"
              @click="handleAddToCart"
            >
              {{ stockQty === 0 ? '缺貨' : '加入購物車' }}
            </v-btn>
            <v-btn
              :icon="true"
              :color="isWishlisted ? 'error' : undefined"
              variant="outlined"
              @click="toggleWishlist"
            >
              <v-icon>{{ isWishlisted ? 'mdi-heart' : 'mdi-heart-outline' }}</v-icon>
            </v-btn>
          </div>
        </v-col>
      </v-row>

      <!-- 標籤頁 -->
      <v-card class="mt-6">
        <v-tabs v-model="activeTab" color="primary">
          <v-tab value="desc">產品詳情</v-tab>
          <v-tab value="reviews">評論 ({{ reviews.length }})</v-tab>
          <v-tab value="specs">規格</v-tab>
        </v-tabs>

        <v-card-text>
          <v-tabs-window v-model="activeTab">
            <!-- 描述 -->
            <v-tabs-window-item value="desc">
              <p style="white-space: pre-line">{{ product.longDescription || product.description }}</p>
            </v-tabs-window-item>

            <!-- 評論 -->
            <v-tabs-window-item value="reviews">
              <!-- 寫評論 (登入後顯示) -->
              <v-card v-if="isAuthenticated" variant="outlined" class="mb-4 pa-3">
                <p class="text-subtitle-1 font-weight-medium mb-2">寫評論</p>
                <p class="text-body-2 mb-1">評分</p>
                <v-rating v-model="reviewRating" color="amber" class="mb-3" />
                <v-textarea
                  v-model="reviewText"
                  label="分享您的使用心得..."
                  variant="outlined"
                  rows="3"
                  class="mb-3"
                />
                <v-btn color="primary" :loading="submittingReview" @click="handleSubmitReview">
                  提交評論
                </v-btn>
              </v-card>

              <!-- 評論列表 -->
              <v-card
                v-for="review in reviews"
                :key="review.id"
                variant="outlined"
                class="mb-3 pa-3"
              >
                <div class="d-flex justify-space-between mb-1">
                  <span class="font-weight-medium">{{ review.user?.name || review.user?.username }}</span>
                  <span class="text-body-2 text-medium-emphasis">{{ formatDate(review.createdAt) }}</span>
                </div>
                <v-rating :model-value="review.rating" readonly density="compact" color="amber" size="small" class="mb-1" />
                <p class="text-body-1">{{ review.comment }}</p>
              </v-card>

              <p v-if="reviews.length === 0" class="text-medium-emphasis">暫無評論</p>
            </v-tabs-window-item>

            <!-- 規格 -->
            <v-tabs-window-item value="specs">
              <v-row>
                <v-col cols="6">
                  <p class="text-body-2 text-medium-emphasis">品牌</p>
                  <p>{{ product.brand || '未指定' }}</p>
                </v-col>
                <v-col cols="6">
                  <p class="text-body-2 text-medium-emphasis">型號</p>
                  <p>{{ product.model || '未指定' }}</p>
                </v-col>
                <v-col cols="6">
                  <p class="text-body-2 text-medium-emphasis">重量</p>
                  <p>{{ product.weight || '未指定' }}</p>
                </v-col>
                <v-col cols="6">
                  <p class="text-body-2 text-medium-emphasis">尺寸</p>
                  <p>{{ product.dimensions || '未指定' }}</p>
                </v-col>
              </v-row>
            </v-tabs-window-item>
          </v-tabs-window>
        </v-card-text>
      </v-card>
    </template>
  </v-container>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useToast } from 'vue-toastification'
import { useAuthStore } from '../stores/authStore.js'
import api, { cartAPI, wishlistAPI } from '../services/api.js'

const route = useRoute()
const router = useRouter()
const toast = useToast()
const authStore = useAuthStore()
const isAuthenticated = computed(() => authStore.isAuthenticated)

const product = ref(null)
const loading = ref(false)
const error = ref(false)
const reviews = ref([])
const selectedImage = ref(0)
const selectedVariant = ref(null)
const quantity = ref(1)
const activeTab = ref('desc')
const reviewText = ref('')
const reviewRating = ref(5)
const isWishlisted = ref(false)
const addingToCart = ref(false)
const submittingReview = ref(false)

const currentPrice = computed(() =>
  selectedVariant.value ? selectedVariant.value.price : product.value?.price || 0
)
const stockQty = computed(() =>
  selectedVariant.value ? selectedVariant.value.stock : product.value?.stock || 0
)

function formatDate(d) {
  return d ? new Date(d).toLocaleDateString('zh-TW') : ''
}

async function loadProduct() {
  loading.value = true
  error.value = false
  try {
    const res = await api.get(`/products/${route.params.id}`)
    product.value = res.data
    if (res.data.variants?.length > 0) selectedVariant.value = res.data.variants[0]
  } catch {
    error.value = true
  } finally {
    loading.value = false
  }
}

async function loadWishlistStatus() {
  if (!isAuthenticated.value) return
  isWishlisted.value = false
  try {
    const response = await wishlistAPI.getWishlist()
    const items = response.data?.items ?? []
    const productId = Number(route.params.id)
    isWishlisted.value = items.some(item => item.productId === productId)
  } catch {}
}

async function loadReviews() {
  try {
    const res = await api.get(`/products/${route.params.id}/reviews`)
    reviews.value = Array.isArray(res.data) ? res.data : res.data?.data || []
  } catch {}
}

async function handleAddToCart() {
  if (!isAuthenticated.value) { router.push('/login'); return }
  addingToCart.value = true
  try {
    await cartAPI.addToCart(authStore.user.id, {
      productId: product.value.id,
      variantId: selectedVariant.value?.id,
      quantity: quantity.value,
    })
    toast.success('已添加到購物車')
  } catch (err) {
    toast.error(err.response?.data?.message || '添加失敗')
  } finally {
    addingToCart.value = false
  }
}

async function toggleWishlist() {
  if (!isAuthenticated.value) { router.push('/login'); return }
  try {
    if (isWishlisted.value) {
      await api.delete(`/wishlist/items/${route.params.id}`)
      toast.success('已從願望清單移除')
    } else {
      await api.post('/wishlist/items', null, { params: { productId: route.params.id } })
      toast.success('已加入願望清單')
    }
    isWishlisted.value = !isWishlisted.value
  } catch {}
}

async function handleSubmitReview() {
  if (!reviewText.value.trim()) { toast.error('請輸入評論內容'); return }
  submittingReview.value = true
  try {
    await api.post(`/products/${route.params.id}/reviews`, {
      rating: reviewRating.value,
      comment: reviewText.value,
    })
    toast.success('評論提交成功')
    reviewText.value = ''
    reviewRating.value = 5
    await loadReviews()
  } catch (err) {
    toast.error(err.response?.data?.message || '評論提交失敗')
  } finally {
    submittingReview.value = false
  }
}

onMounted(() => {
  loadProduct()
  loadReviews()
  loadWishlistStatus()
})

watch(() => route.params.id, () => {
  loadProduct()
  loadReviews()
  loadWishlistStatus()
})
</script>
