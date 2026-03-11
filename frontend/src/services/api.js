import axios from 'axios';
import toast from 'react-hot-toast';

const api = axios.create({
  baseURL: '/api',
  timeout: 10000,
  withCredentials: true,
  headers: {
    'Content-Type': 'application/json',
  },
});

// 請求攔截器
api.interceptors.request.use(
  (config) => {
    // 先嘗試從簡單的 token storage 讀取
    let token = localStorage.getItem('token');
    
    // 如果沒有，再嘗試從 auth-storage 讀取
    if (!token) {
      const authStorage = localStorage.getItem('auth-storage');
      if (authStorage) {
        try {
          const parsed = JSON.parse(authStorage);
          if (parsed.state?.token) {
            token = parsed.state.token;
          }
        } catch (error) {
          console.error('Failed to parse auth-storage:', error);
        }
      }
    }
    
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// 響應攔截器
api.interceptors.response.use(
  (response) => {
    return response;
  },
  (error) => {
    // 檢查請求 URL 是否為登入相關 (Login 或 Register 後的自動登入)
    // 注意: error.config 可能未定義 (例如網路錯誤)，需做保護
    const requestUrl = error.config?.url || '';
    const isAuthRequest = requestUrl.includes('/auth/login') || requestUrl.includes('/auth/register');

    const isAiRequest = requestUrl.includes('/ai/');

    if (error.response?.status === 401 && !isAuthRequest) {
      // Token 過期或無效，且不是正在嘗試登入或註冊
      localStorage.removeItem('auth-storage');
      window.location.href = '/login';
      toast.error('登入已過期，請重新登入');
    } else if (!isAiRequest && error.response?.status >= 500) {
      // AI 端點的 5xx 錯誤由各 page 自行處理（避免重複 toast）
      toast.error('服務器錯誤，請稍後再試');
    }
    
    return Promise.reject(error);
  }
);

// 認證相關 API
export const authAPI = {
  register: (data) => api.post('/auth/register', data),
  login: (data) => api.post('/auth/login', data),
  refresh: () => api.post('/auth/refresh'),
  getUsers: () => api.get('/auth/users'), // 取得所有用戶列表
};

// 產品相關 API
export const productAPI = {
  getProducts: (params) => api.get('/products', { params }),
  getProduct: (id) => api.get(`/products/${id}`),
  searchProducts: (keyword) => api.get(`/products/search?keyword=${keyword}`),
  getProductsByCategory: (category) => api.get(`/products/category/${category}`),
  getFeaturedProducts: () => api.get('/products/featured'),
  getProductReviews: (productId) => api.get(`/products/${productId}/reviews`),
  addReview: (productId, review) => api.post(`/products/${productId}/reviews`, review),
};

// 購物車相關 API
export const cartAPI = {
  getCart: (userId) => api.get(`/cart/${userId}`),
  addToCart: (userId, data) => api.post(`/cart/${userId}/items?productId=${data.productId}&quantity=${data.quantity}`),
  updateCartItem: (userId, itemId, quantity) => api.put(`/cart/${userId}/items/${itemId}?quantity=${quantity}`),
  removeFromCart: (userId, itemId) => api.delete(`/cart/${userId}/items/${itemId}`),
  clearCart: (userId) => api.delete(`/cart/${userId}`),
  getCartTotal: (userId) => api.get(`/cart/${userId}/total`),
};

// 聊天室相關 API
export const chatAPI = {
  sendMessage: (data) => api.post('/chat/send', data),
  getChatHistory: (userId1, userId2) => api.get(`/chat/history?user1=${userId1}&user2=${userId2}`),
  getUnreadMessages: (userId) => api.get(`/chat/unread/${userId}`),
  getUnreadCount: (userId) => api.get(`/chat/unread-count/${userId}`),
  markAsRead: (messageId) => api.put(`/chat/read/${messageId}`),
  markChatAsRead: (receiverId, senderId) => api.put(`/chat/read-chat?receiverId=${receiverId}&senderId=${senderId}`),
  getSentMessages: (userId) => api.get(`/chat/sent/${userId}`),
  getReceivedMessages: (userId) => api.get(`/chat/received/${userId}`),
  getAllUserMessages: (userId) => api.get(`/chat/all/${userId}`),
  deleteMessage: (messageId) => api.delete(`/chat/${messageId}`),
  getMessageById: (messageId) => api.get(`/chat/message/${messageId}`),
};

// 訂單相關 API
export const orderAPI = {
  createOrder: (params) => api.post(`/orders${params}`),
  getOrders: () => api.get('/orders'),
  getOrder: (orderId) => api.get(`/orders/${orderId}`),
  cancelOrder: (orderId, userId) => api.post(`/orders/${orderId}/cancel`, null, { params: { userId } }),
  getUserOrders: (userId) => api.get(`/orders/user/${userId}`),
};

// 願望清單相關 API
export const wishlistAPI = {
  getWishlist: () => api.get('/wishlist'),
  addToWishlist: (productId) => api.post('/wishlist/items', null, { params: { productId } }),
  removeFromWishlist: (productId) => api.delete(`/wishlist/items/${productId}`),
};

// Passkeys / WebAuthn API
export const passkeyAPI = {
  // 註冊流程
  startRegistration: (username, displayName) =>
    api.post('/passkeys/registration/start', { username, displayName }),
  finishRegistration: (username, displayName, rawResponse) =>
    api.post('/passkeys/registration/finish', { username, displayName, rawResponse }),
  // 認證流程（username 可為 null，支援 discoverable credential）
  startAssertion: (username = null) =>
    api.post('/passkeys/assertion/start', null, { params: username ? { username } : {} }),
  finishAssertion: (rawResponse, username = null) =>
    api.post('/passkeys/assertion/finish', { username, rawResponse }),
  // 管理
  listPasskeys: () => api.get('/passkeys'),
  deletePasskey: (credentialId) => api.delete(`/passkeys/${encodeURIComponent(credentialId)}`),
};

// 用戶相關 API
export const userAPI = {
  getProfile: () => api.get('/user/profile'),
  updateProfile: (data) => api.put('/user/profile', data),
  changePassword: (data) => api.put('/user/password', data),
  uploadAvatar: (formData) => api.post('/user/avatar', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  }),
};

// TODO: AI 功能暫時停用
// AI 助手相關 API（Ollama LLM）
// export const aiAPI = {
//   // 通用購物助手對話（LLM 推理最多 5 分鐘，確保不提前超時）
//   chat: (message, history = [], systemPrompt = null) =>
//     api.post('/ai/chat', { message, history, systemPrompt }, { timeout: 300000 }),
//   // 根據意圖推薦商品
//   recommend: (intent) =>
//     api.post('/ai/recommend', { intent }, { timeout: 300000 }),
//   // 自然語言搜尋關鍵字萃取
//   searchAssist: (q) =>
//     api.get(`/ai/search?q=${encodeURIComponent(q)}`, { timeout: 120000 }),
//   // 商品 AI 摘要
//   productSummary: (id) =>
//     api.get(`/ai/products/${id}/summary`, { timeout: 120000 }),
//   // 健康檢查（輕量，使用 /api/tags 不做推理，3 秒應足夠）
//   health: () => api.get('/ai/health', { timeout: 8000 }),
// };

export default api;
