// 願望清單事件管理器
class WishlistEventManager {
  constructor() {
    this.listeners = [];
  }

  subscribe(callback) {
    this.listeners.push(callback);
    return () => {
      this.listeners = this.listeners.filter(listener => listener !== callback);
    };
  }

  notify() {
    this.listeners.forEach(callback => callback());
  }
}

export const wishlistEvents = new WishlistEventManager();
