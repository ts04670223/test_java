<template>
  <v-container fluid class="pa-4">
    <v-row>
      <!-- 側邊篩選欄（桌面版） -->
      <v-col cols="12" md="3" lg="2" class="d-none d-md-block">
        <v-card elevation="0" variant="outlined" class="sticky-top">
          <v-card-title class="text-subtitle-1">篩選</v-card-title>
          <v-card-text>
            <v-text-field
              v-model="searchTerm"
              label="搜尋商品"
              prepend-inner-icon="mdi-magnify"
              variant="outlined"
              density="compact"
              clearable
              class="mb-3"
            />
            <v-select
              v-model="selectedCategory"
              :items="categoryItems"
              label="分類"
              variant="outlined"
              density="compact"
            />
          </v-card-text>

          <!-- 購物車摘要 -->
          <v-divider />
          <v-card-title class="text-subtitle-1 d-flex align-center">
            <v-icon size="small" class="mr-1">mdi-cart</v-icon>
            購物車
            <v-spacer />
            <v-chip size="small" color="primary">{{ cartItems.length }}</v-chip>
          </v-card-title>
          <v-card-text class="pa-2">
            <template v-if="cartItems.length > 0">
              <v-list density="compact" class="overflow-y-auto" style="max-height: 280px">
                <v-list-item
                  v-for="item in cartItems"
                  :key="item.id"
                  class="px-0"
                >
                  <template v-slot:title>
                    <span class="text-body-2">{{ item.productName || item.product?.name }}</span>
                  </template>
                  <template v-slot:subtitle>
                    <span class="text-caption">x {{ item.quantity }}</span>
                  </template>
                  <template v-slot:append>
                    <div class="d-flex align-center ga-1">
                      <v-btn icon size="x-small" variant="text" @click="updateCartItemQuantity(item.id, item.quantity - 1)">
                        <v-icon size="12">mdi-minus</v-icon>
                      </v-btn>
                      <span class="text-caption">{{ item.quantity }}</span>
                      <v-btn icon size="x-small" variant="text" @click="updateCartItemQuantity(item.id, item.quantity + 1)">
                        <v-icon size="12">mdi-plus</v-icon>
                      </v-btn>
                      <v-btn icon size="x-small" variant="text" color="error" @click="removeFromCart(item.id)">
                        <v-icon size="12">mdi-delete</v-icon>
                      </v-btn>
                    </div>
                  </template>
                </v-list-item>
              </v-list>
              <v-btn color="primary" block size="small" class="mt-2" @click="goToCheckout">結帳</v-btn>
              <v-btn color="error" variant="outlined" block size="small" class="mt-1" @click="clearCartDialog = true">清空</v-btn>
            </template>
            <div v-else class="text-center text-body-2 text-medium-emphasis py-4">
              購物車是空的
            </div>
          </v-card-text>
        </v-card>
      </v-col>

      <!-- 商品列表 -->
      <v-col cols="12" md="9" lg="10">
        <!-- 手機版篩選按鈕 -->
        <div class="d-flex d-md-none gap-2 mb-4">
          <v-btn variant="outlined" prepend-icon="mdi-filter" @click="filterDialog = true" block>
            篩選與搜尋
          </v-btn>
          <v-btn variant="outlined" prepend-icon="mdi-cart" @click="cartDrawer = true">
            <v-badge :content="cartItems.length" color="error" :model-value="cartItems.length > 0">
              購物車
            </v-badge>
          </v-btn>
        </div>

        <!-- 載入中 -->
        <div v-if="loading" class="d-flex justify-center py-12">
          <v-progress-circular indeterminate color="primary" size="48" />
        </div>

        <!-- 商品網格 -->
        <v-row v-else>
          <v-col v-if="filteredProducts.length === 0" cols="12" class="text-center py-12">
            <v-icon size="64" color="grey">mdi-package-variant-remove</v-icon>
            <p class="text-body-1 text-medium-emphasis mt-2">沒有找到符合的商品</p>
          </v-col>

          <v-col
            v-for="product in filteredProducts"
            :key="product.id"
            cols="12" sm="6" lg="4"
          >
            <v-card height="100%" class="d-flex flex-column">
              <!-- 商品圖片 -->
              <div class="position-relative bg-grey-lighten-4" style="aspect-ratio: 1">
                <v-img
                  v-if="product.imageUrl"
                  :src="product.imageUrl"
                  :alt="product.name"
                  cover
                  height="100%"
                />
                <div v-else class="d-flex align-center justify-center fill-height">
                  <v-icon size="64" color="grey">mdi-image-off</v-icon>
                </div>
                <!-- 缺貨遮罩 -->
                <div
                  v-if="product.stock === 0"
                  class="position-absolute inset-0 d-flex align-center justify-center bg-black-50"
                  style="background: rgba(0,0,0,0.5)"
                >
                  <v-chip color="grey" size="large">缺貨</v-chip>
                </div>
              </div>

              <v-card-title class="text-body-1 pb-1">
                <span class="text-truncate d-block">{{ product.name }}</span>
              </v-card-title>
              <v-card-subtitle v-if="product.category">
                <v-chip size="x-small" variant="outlined">{{ product.category }}</v-chip>
              </v-card-subtitle>

              <v-card-text class="flex-grow-1 py-2">
                <p class="text-body-2 text-medium-emphasis line-clamp-2">{{ product.description }}</p>
                <div class="d-flex align-center justify-space-between mt-3">
                  <span class="text-h6 font-weight-bold text-primary">
                    ${{ parseFloat(product.price || 0).toFixed(0) }}
                  </span>
                  <span class="text-caption text-medium-emphasis">庫存: {{ product.stock }}</span>
                </div>
              </v-card-text>

              <v-card-actions class="pa-3 pt-0">
                <v-btn
                  color="primary"
                  variant="flat"
                  class="flex-grow-1"
                  :disabled="product.stock === 0"
                  @click="addToCart(product.id)"
                  prepend-icon="mdi-cart-plus"
                >
                  {{ product.stock === 0 ? '缺貨' : '加入購物車' }}
                </v-btn>
                <v-btn
                  icon
                  variant="outlined"
                  size="small"
                  @click="toggleWishlist(product.id)"
                >
                  <v-icon :color="wishlistedIds.has(product.id) ? 'red' : 'grey'">
                    {{ wishlistedIds.has(product.id) ? 'mdi-heart' : 'mdi-heart-outline' }}
                  </v-icon>
                </v-btn>
              </v-card-actions>
            </v-card>
          </v-col>
        </v-row>
      </v-col>
    </v-row>

    <!-- 手機版篩選 Dialog -->
    <v-dialog v-model="filterDialog" max-width="400">
      <v-card>
        <v-card-title>篩選與搜尋</v-card-title>
        <v-card-text>
          <v-text-field
            v-model="searchTerm"
            label="搜尋商品"
            prepend-inner-icon="mdi-magnify"
            variant="outlined"
            clearable
            class="mb-3"
          />
          <v-select
            v-model="selectedCategory"
            :items="categoryItems"
            label="分類"
            variant="outlined"
          />
        </v-card-text>
        <v-card-actions>
          <v-spacer />
          <v-btn @click="filterDialog = false">確定</v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>

    <!-- 手機版購物車 Drawer -->
    <v-navigation-drawer v-model="cartDrawer" location="right" temporary width="300">
      <v-list-item title="購物車" nav>
        <template v-slot:append>
          <v-btn icon size="small" @click="cartDrawer = false">
            <v-icon>mdi-close</v-icon>
          </v-btn>
        </template>
      </v-list-item>
      <v-divider />
      <v-list density="compact">
        <v-list-item v-for="item in cartItems" :key="item.id">
          <template v-slot:title>
            <span class="text-body-2">{{ item.productName || item.product?.name }}</span>
          </template>
          <template v-slot:append>
            <div class="d-flex align-center ga-1">
              <v-btn icon size="x-small" variant="text" @click="updateCartItemQuantity(item.id, item.quantity - 1)">
                <v-icon size="12">mdi-minus</v-icon>
              </v-btn>
              <span class="text-caption">{{ item.quantity }}</span>
              <v-btn icon size="x-small" variant="text" @click="updateCartItemQuantity(item.id, item.quantity + 1)">
                <v-icon size="12">mdi-plus</v-icon>
              </v-btn>
            </div>
          </template>
        </v-list-item>
      </v-list>
      <template v-slot:append>
        <div class="pa-3">
          <v-btn color="primary" block @click="goToCheckout" :disabled="cartItems.length === 0">結帳</v-btn>
        </div>
      </template>
    </v-navigation-drawer>

    <!-- 清空購物車確認 -->
    <v-dialog v-model="clearCartDialog" max-width="360">
      <v-card>
        <v-card-title>清空購物車</v-card-title>
        <v-card-text>確定要清空購物車內的所有商品嗎？</v-card-text>
        <v-card-actions>
          <v-spacer />
          <v-btn @click="clearCartDialog = false">取消</v-btn>
          <v-btn color="error" @click="clearCart">清空</v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>
  </v-container>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { useToast } from 'vue-toastification'
import { productAPI, cartAPI, chatAPI, wishlistAPI } from '../services/api.js'
import { useAuthStore } from '../stores/authStore.js'
import { cartEvents } from '../utils/cartEvents.js'

const router = useRouter()
const toast = useToast()
const authStore = useAuthStore()
const user = computed(() => authStore.user)

const products = ref([])
const cartItems = ref([])
const loading = ref(true)
const searchTerm = ref('')
const selectedCategory = ref('all')
const categories = ref([])
const wishlistedIds = ref(new Set())
const filterDialog = ref(false)
const cartDrawer = ref(false)
const clearCartDialog = ref(false)

let interval = null

const categoryItems = computed(() => [
  { title: '全部分類', value: 'all' },
  ...categories.value.map(c => ({ title: c, value: c })),
])

const filteredProducts = computed(() => {
  return products.value.filter(p => {
    const matchSearch = !searchTerm.value ||
      p.name?.toLowerCase().includes(searchTerm.value.toLowerCase()) ||
      p.description?.toLowerCase().includes(searchTerm.value.toLowerCase())
    const matchCategory = selectedCategory.value === 'all' || p.category === selectedCategory.value
    return matchSearch && matchCategory
  })
})

async function loadProducts() {
  try {
    const response = await productAPI.getProducts()
    const data = response.data?.data || response.data || {}
    const list = Array.isArray(data) ? data : (data.content || [])
    products.value = list
    categories.value = [...new Set(list.map(p => p.category).filter(Boolean))]
    loading.value = false
  } catch {
    products.value = []
    loading.value = false
  }
}

async function loadCartItems() {
  if (!user.value?.id) return
  try {
    const response = await cartAPI.getCart(user.value.id)
    const cartData = response.data?.data || response.data
    cartItems.value = cartData?.items || []
  } catch {}
}

async function addToCart(productId) {
  if (!user.value?.id) { toast.error('請先登入'); return }
  try {
    await cartAPI.addToCart(user.value.id, { productId, quantity: 1 })
    await loadCartItems()
    cartEvents.notify()
    toast.success('已加入購物車')
  } catch (err) {
    toast.error(err.response?.data?.error || '加入購物車失敗')
  }
}

async function updateCartItemQuantity(itemId, quantity) {
  if (!user.value?.id) return
  if (quantity < 1) { await removeFromCart(itemId); return }
  try {
    await cartAPI.updateCartItem(user.value.id, itemId, quantity)
    await loadCartItems()
    cartEvents.notify()
  } catch (err) {
    toast.error(err.response?.data?.error || '更新失敗')
  }
}

async function removeFromCart(itemId) {
  if (!user.value?.id) return
  try {
    await cartAPI.removeFromCart(user.value.id, itemId)
    await loadCartItems()
    cartEvents.notify()
  } catch {}
}

async function clearCart() {
  if (!user.value?.id) return
  clearCartDialog.value = false
  try {
    await cartAPI.clearCart(user.value.id)
    await loadCartItems()
    toast.success('購物車已清空')
  } catch {
    toast.error('清空購物車失敗')
  }
}

async function loadWishlist() {
  if (!user.value?.id) return
  try {
    const response = await wishlistAPI.getWishlist()
    const items = response.data?.items ?? response.data ?? []
    wishlistedIds.value = new Set(items.map(item => item.productId ?? item.product?.id))
  } catch {}
}

async function toggleWishlist(productId) {
  if (!user.value?.id) { toast.error('請先登入'); return }
  try {
    if (wishlistedIds.value.has(productId)) {
      await wishlistAPI.removeFromWishlist(productId)
      wishlistedIds.value = new Set([...wishlistedIds.value].filter(id => id !== productId))
      toast.success('已從願望清單移除')
    } else {
      await wishlistAPI.addToWishlist(productId)
      wishlistedIds.value = new Set([...wishlistedIds.value, productId])
      toast.success('已加入願望清單')
    }
  } catch (err) {
    toast.error(err.response?.data?.message || '操作失敗')
  }
}

function goToCheckout() {
  if (cartItems.value.length === 0) { toast.error('購物車是空的！'); return }
  router.push('/checkout')
}

onMounted(() => {
  loadProducts()
  loadCartItems()
  loadWishlist()
  interval = setInterval(loadCartItems, 10000)
  cartEvents.subscribe(() => loadCartItems())
})

onUnmounted(() => {
  if (interval) clearInterval(interval)
})
</script>
