import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import toast from 'react-hot-toast';
import { ShoppingCart, MessageCircle, LogOut, User, Search, Trash2, Menu, X, Filter, Bot, Heart } from 'lucide-react';

import { productAPI, cartAPI, chatAPI, wishlistAPI } from '../services/api';
import { useAuthStore } from '../stores/authStore';
import { cartEvents } from '../utils/cartEvents';

import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Badge } from '@/components/ui/badge';
import { Card, CardContent, CardFooter, CardHeader, CardTitle } from '@/components/ui/card';
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
  DialogFooter,
  DialogClose,
} from '@/components/ui/dialog';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import { Label } from '@/components/ui/label';

function Shop() {
  const { user, logout } = useAuthStore();
  const navigate = useNavigate();
  const [products, setProducts] = useState([]);
  const [cart, setCart] = useState(null);
  const [cartItems, setCartItems] = useState([]);
  const [loading, setLoading] = useState(true);
  const [searchTerm, setSearchTerm] = useState('');
  const [selectedCategory, setSelectedCategory] = useState('all');
  const [categories, setCategories] = useState([]);
  const [unreadCount, setUnreadCount] = useState(0); 
  const [showMobileCart, setShowMobileCart] = useState(false); 
  const [showMobileFilter, setShowMobileFilter] = useState(false);
  const [openClearCartDialog, setOpenClearCartDialog] = useState(false);
  const [wishlistedIds, setWishlistedIds] = useState(new Set());

  useEffect(() => {
    loadProducts();
    loadCart();
    loadUnreadCount();
    loadWishlist();
    const interval = setInterval(loadUnreadCount, 10000);
    return () => clearInterval(interval);
  }, []);

  useEffect(() => {
    if (cart) {
      loadCartItems();
    }
  }, [cart]);

  const loadProducts = async () => {
    try {
      const response = await productAPI.getProducts();
      const productsData = response.data?.data || response.data || [];
      setProducts(Array.isArray(productsData) ? productsData : []);
      const uniqueCategories = [...new Set(productsData.map(p => p.category).filter(c => c))];
      setCategories(uniqueCategories);
      setLoading(false);
    } catch (error) {
      console.error('載入商品失敗:', error);
      setProducts([]);
      setLoading(false);
    }
  };

  const loadCart = async () => {
    if (!user?.id) return;
    try {
      const response = await cartAPI.getCart(user.id);
      setCart(response.data?.data || response.data);
    } catch (error) {
      console.error('載入購物車失敗:', error);
    }
  };

  const loadCartItems = async () => {
    if (!user?.id) return;
    try {
      const response = await cartAPI.getCart(user.id);
      const cartData = response.data?.data || response.data;
      setCartItems(cartData?.items || []);
      setCart(cartData); 
    } catch (error) {
      console.error('載入購物車項目失敗:', error);
    }
  };

  const addToCart = async (productId) => {
    if (!user?.id) {
      toast.error('請先登入');
      return;
    }
    try {
      await cartAPI.addToCart(user.id, { productId, quantity: 1 });
      await loadCartItems();
      cartEvents.notify();
      toast.success('已加入購物車');
    } catch (error) {
      toast.error(error.response?.data?.error || '加入購物車失敗');
    }
  };

  const updateCartItemQuantity = async (itemId, quantity) => {
    if (!user?.id) return;
    if (quantity < 1) {
      await removeFromCart(itemId);
      return;
    }
    try {
      await cartAPI.updateCartItem(user.id, itemId, quantity);
      await loadCartItems();
      cartEvents.notify();
    } catch (error) {
      toast.error(error.response?.data?.error || '更新失敗');
    }
  };

  const removeFromCart = async (itemId) => {
    if (!user?.id) return;
    try {
      await cartAPI.removeFromCart(user.id, itemId);
      await loadCartItems();
      cartEvents.notify();
    } catch (error) {
      console.error('移除失敗:', error);
    }
  };

  const clearCart = async () => {
    if (!user?.id) return;
    setOpenClearCartDialog(false);
    try {
      await cartAPI.clearCart(user.id);
      await loadCartItems();
      toast.success('購物車已清空');
    } catch (error) {
      toast.error('清空購物車失敗');
    }
  };

  const loadUnreadCount = async () => {
    if (!user?.id) return;
    try {
      const response = await chatAPI.getUnreadCount(user.id);
      setUnreadCount(response.data?.unreadCount || response.data?.data?.unreadCount || 0);
    } catch (error) { console.error(error); }
  };

  const loadWishlist = async () => {
    if (!user?.id) return;
    try {
      const response = await wishlistAPI.getWishlist();
      const items = response.data?.items ?? response.data ?? [];
      const ids = new Set(items.map(item => item.productId ?? item.product?.id));
      setWishlistedIds(ids);
    } catch (error) {
      // 未登入或其他錯誤，不影響頁面
    }
  };

  const toggleWishlist = async (productId) => {
    if (!user?.id) {
      toast.error('請先登入');
      return;
    }
    try {
      if (wishlistedIds.has(productId)) {
        await wishlistAPI.removeFromWishlist(productId);
        setWishlistedIds(prev => { const next = new Set(prev); next.delete(productId); return next; });
        toast.success('已從願望清單移除');
      } else {
        await wishlistAPI.addToWishlist(productId);
        setWishlistedIds(prev => new Set(prev).add(productId));
        toast.success('已加入願望清單');
      }
    } catch (error) {
      toast.error(error.response?.data?.message || '操作失敗');
    }
  };

  const goToCheckout = () => {
    if (cartItems.length === 0) {
      toast.error('購物車是空的!');
      return;
    }
    navigate('/checkout');
  };

  const filteredProducts = Array.isArray(products) ? products.filter(product => {
    const matchesSearch = product.name?.toLowerCase().includes(searchTerm.toLowerCase()) ||
      (product.description || '').toLowerCase().includes(searchTerm.toLowerCase());
    const matchesCategory = selectedCategory === 'all' || !selectedCategory || product.category === selectedCategory;
    return matchesSearch && matchesCategory && product.active;
  }) : [];

  const cartTotal = cart?.total || cartItems.reduce((sum, item) => {
    const itemTotal = item.subtotal || (parseFloat(item.price || 0) * (item.quantity || 0));
    return sum + itemTotal;
  }, 0);
  const cartCount = cartItems.reduce((sum, item) => sum + (item.quantity || 0), 0);

  if (loading) return <div className="flex h-screen items-center justify-center">載入中...</div>;

  return (
    <div className="flex min-h-screen flex-col bg-background">
      {/* Navigation */}
      <header className="sticky top-0 z-40 w-full border-b bg-background/95 backdrop-blur supports-[backdrop-filter]:bg-background/60">
        <div className="container flex h-16 items-center px-4">
          <div className="flex flex-1 items-center justify-between space-x-2 md:justify-end">
             <div className="w-full flex-1 md:w-auto md:flex-none">
                {/* Mobile Search placeholder or hidden */}
             </div>
             <nav className="flex items-center space-x-2">
                {/* <Button variant="ghost" size="icon" onClick={() => navigate('/ai')} title="AI 助手" className="relative">*/}{/* TODO: AI 功能暫時停用 */}
                {/*   <Bot className="h-5 w-5" />*/}
                {/* </Button>*/}
                <Button variant="ghost" size="icon" onClick={() => navigate('/chat')} className="relative">
                  <MessageCircle className="h-5 w-5" />
                  {unreadCount > 0 && (
                    <Badge variant="destructive" className="absolute -right-1 -top-1 px-1 min-w-[1.25rem] h-5 justify-center">
                      {unreadCount}
                    </Badge>
                  )}
                </Button>
                <Button variant="ghost" size="icon" onClick={() => setShowMobileCart(true)} className="md:hidden relative">
                   <ShoppingCart className="h-5 w-5" />
                   {cartCount > 0 && (
                    <Badge className="absolute -right-1 -top-1 px-1 min-w-[1.25rem] h-5 justify-center">
                      {cartCount}
                    </Badge>
                  )}
                </Button>
                <div className="hidden md:flex items-center gap-2">
                   <span className="text-sm font-medium"> {user?.name || user?.username}</span>
                   <Button variant="outline" size="sm" onClick={() => { logout(); navigate('/login'); }}>
                     登出
                   </Button>
                </div>
             </nav>
          </div>
        </div>
      </header>

      <div className="container flex-1 items-start md:grid md:grid-cols-[240px_1fr] md:gap-6 md:px-4 md:py-6">
        {/* Sidebar */}
        <aside className="fixed top-14 z-30 -ml-2 hidden h-[calc(100vh-3.5rem)] w-full shrink-0 overflow-y-auto border-r md:sticky md:block">
          <div className="py-6 pr-6 lg:py-8">
            <h3 className="mb-4 text-lg font-semibold">篩選與搜尋</h3>
            <div className="space-y-4">
               <div className="space-y-2">
                 <Label>搜尋商品</Label>
                 <div className="relative">
                   <Search className="absolute left-2 top-2.5 h-4 w-4 text-muted-foreground" />
                   <Input 
                     placeholder="輸入關鍵字..." 
                     className="pl-8"
                     value={searchTerm}
                     onChange={(e) => setSearchTerm(e.target.value)}
                   />
                 </div>
               </div>
               <div className="space-y-2">
                 <Label>商品分類</Label>
                 <Select value={selectedCategory} onValueChange={setSelectedCategory}>
                    <SelectTrigger>
                      <SelectValue placeholder="選擇分類" />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value="all">全部商品</SelectItem>
                      {categories.map(cat => (
                        <SelectItem key={cat} value={cat}>{cat}</SelectItem>
                      ))}
                    </SelectContent>
                 </Select>
               </div>
            </div>

            <div className="mt-8 border-t pt-6">
               <h3 className="mb-4 text-lg font-semibold flex items-center gap-2">
                 <ShoppingCart className="h-5 w-5" /> 購物車
               </h3>
               <div className="space-y-4">
                  <div className="flex justify-between text-sm">
                    <span>商品數量:</span>
                    <span className="font-medium">{cartCount}</span>
                  </div>
                  <div className="flex justify-between text-sm">
                    <span>總金額:</span>
                    <span className="font-medium text-primary"></span>
                  </div>
                  
                  {cartItems.length > 0 ? (
                    <div className="space-y-3">
                      <div className="max-h-[300px] overflow-y-auto space-y-2 pr-2">
                        {cartItems.map(item => (
                          <div key={item.id} className="flex justify-between items-start text-sm border-b pb-2">
                             <div className="flex-1">
                               <p className="font-medium truncate">{item.productName || item.product?.name}</p>
                               <p className="text-muted-foreground text-xs"> x {item.quantity}</p>
                             </div>
                             <div className="flex items-center gap-1">
                               <Button variant="ghost" size="icon" className="h-6 w-6" onClick={() => updateCartItemQuantity(item.id, item.quantity - 1)}>-</Button>
                               <span className="w-4 text-center">{item.quantity}</span>
                               <Button variant="ghost" size="icon" className="h-6 w-6" onClick={() => updateCartItemQuantity(item.id, item.quantity + 1)}>+</Button>
                               <Button variant="ghost" size="icon" className="h-6 w-6 text-destructive" onClick={() => removeFromCart(item.id)}>
                                 <Trash2 className="h-3 w-3" />
                               </Button>
                             </div>
                          </div>
                        ))}
                      </div>
                      <div className="grid gap-2">
                        <Button className="w-full" onClick={goToCheckout}>結帳</Button>
                        <Button variant="outline" className="w-full" onClick={() => setOpenClearCartDialog(true)}>清空</Button>
                      </div>
                    </div>
                  ) : (
                    <div className="text-center py-6 text-muted-foreground text-sm">
                      購物車是空的
                    </div>
                  )}
               </div>
            </div>
          </div>
        </aside>

        {/* Main Content */}
        <main className="py-6">
           <div className="flex items-center gap-2 mb-6 md:hidden">
             <Button variant="outline" className="w-full" onClick={() => setShowMobileFilter(true)}>
               <Filter className="mr-2 h-4 w-4" /> 篩選與搜尋
             </Button>
           </div>
           <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
             {filteredProducts.length === 0 ? (
               <div className="col-span-full text-center py-12 text-muted-foreground">
                 沒有找到符合的商品
               </div>
             ) : (
               filteredProducts.map(product => (
                 <Card key={product.id} className="overflow-hidden flex flex-col h-full">
                   <div className="aspect-square bg-muted flex items-center justify-center relative">
                     {product.imageUrl ? (
                       <img src={product.imageUrl} alt={product.name} className="object-cover w-full h-full" />
                     ) : (
                       <span className="text-4xl"></span>
                     )}
                     {product.stock === 0 && (
                       <div className="absolute inset-0 bg-background/80 flex items-center justify-center">
                         <Badge variant="secondary" className="text-lg">缺貨</Badge>
                       </div>
                     )}
                   </div>
                   <CardHeader className="p-4">
                     <div className="flex justify-between items-start gap-2">
                       <CardTitle className="text-lg line-clamp-1">{product.name}</CardTitle>
                       {product.category && <Badge variant="outline" className="shrink-0">{product.category}</Badge>}
                     </div>
                   </CardHeader>
                   <CardContent className="p-4 pt-0 flex-1">
                     <p className="text-sm text-muted-foreground line-clamp-2 min-h-[2.5rem]">
                       {product.description}
                     </p>
                     <div className="mt-4 flex items-baseline justify-between">
                       <span className="text-xl font-bold"></span>
                       <span className="text-xs text-muted-foreground">庫存: {product.stock}</span>
                     </div>
                   </CardContent>
                   <CardFooter className="p-4 pt-0 flex gap-2">
                     <Button 
                       className="flex-1" 
                       disabled={product.stock === 0}
                       onClick={() => addToCart(product.id)}
                     >
                       {product.stock === 0 ? '缺貨' : '加入購物車'}
                     </Button>
                     <Button
                       variant="outline"
                       size="icon"
                       onClick={() => toggleWishlist(product.id)}
                       title={wishlistedIds.has(product.id) ? '從願望清單移除' : '加入願望清單'}
                     >
                       <Heart
                         className="h-4 w-4"
                         fill={wishlistedIds.has(product.id) ? 'currentColor' : 'none'}
                         color={wishlistedIds.has(product.id) ? '#ef4444' : 'currentColor'}
                       />
                     </Button>
                   </CardFooter>
                 </Card>
               ))
             )}
           </div>
        </main>
      </div>

      {/* Mobile Filter Overlay */}
      {showMobileFilter && (
        <div className="fixed inset-0 z-50 bg-background/80 backdrop-blur-sm md:hidden">
          <div className="fixed inset-y-0 left-0 z-50 h-full w-full border-r bg-background p-6 shadow-lg sm:max-w-sm">
             <div className="flex items-center justify-between mb-6">
               <h3 className="text-lg font-semibold">篩選與搜尋</h3>
               <Button variant="ghost" size="icon" onClick={() => setShowMobileFilter(false)}>
                 <X className="h-4 w-4" />
               </Button>
             </div>
             <div className="space-y-4">
               <div className="space-y-2">
                 <Label>搜尋商品</Label>
                 <div className="relative">
                   <Search className="absolute left-2 top-2.5 h-4 w-4 text-muted-foreground" />
                   <Input 
                     placeholder="輸入關鍵字..." 
                     className="pl-8"
                     value={searchTerm}
                     onChange={(e) => setSearchTerm(e.target.value)}
                   />
                 </div>
               </div>
               <div className="space-y-2">
                 <Label>商品分類</Label>
                 <Select value={selectedCategory} onValueChange={setSelectedCategory}>
                    <SelectTrigger>
                      <SelectValue placeholder="選擇分類" />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value="all">全部商品</SelectItem>
                      {categories.map(cat => (
                        <SelectItem key={cat} value={cat}>{cat}</SelectItem>
                      ))}
                    </SelectContent>
                 </Select>
               </div>
               <div className="pt-4">
                  <Button className="w-full" onClick={() => setShowMobileFilter(false)}>
                    確認
                  </Button>
               </div>
            </div>
          </div>
        </div>
      )}

      {/* Mobile Cart Overlay (Optional simplified version) */}
      {showMobileCart && (
        <div className="fixed inset-0 z-50 bg-background/80 backdrop-blur-sm md:hidden">
          <div className="fixed inset-y-0 right-0 z-50 h-full w-full border-l bg-background p-6 shadow-lg sm:max-w-sm">
             <div className="flex items-center justify-between mb-6">
               <h3 className="text-lg font-semibold">購物車 ({cartCount})</h3>
               <Button variant="ghost" size="icon" onClick={() => setShowMobileCart(false)}>
                 <X className="h-4 w-4" />
               </Button>
             </div>
             <div className="flex flex-col h-[calc(100%-4rem)]">
                <div className="flex-1 overflow-y-auto -mr-2 pr-2">
                  {cartItems.map(item => (
                    <div key={item.id} className="flex gap-4 mb-4 border-b pb-4">
                       <div className="h-16 w-16 bg-muted rounded flex items-center justify-center bg-gray-100">
                          
                       </div>
                       <div className="flex-1">
                          <h4 className="font-medium line-clamp-1">{item.productName || item.product?.name}</h4>
                          <p className="text-sm text-muted-foreground"></p>
                          <div className="flex items-center gap-2 mt-2">
                             <Button variant="outline" size="icon" className="h-6 w-6" onClick={() => updateCartItemQuantity(item.id, item.quantity - 1)}>-</Button>
                             <span className="text-sm w-4 text-center">{item.quantity}</span>
                             <Button variant="outline" size="icon" className="h-6 w-6" onClick={() => updateCartItemQuantity(item.id, item.quantity + 1)}>+</Button>
                          </div>
                       </div>
                    </div>
                  ))}
                  {cartItems.length === 0 && <p className="text-center text-muted-foreground py-10">購物車是空的</p>}
                </div>
                <div className="mt-4 pt-4 border-t space-y-4">
                  <div className="flex justify-between font-medium">
                    <span>總計</span>
                    <span></span>
                  </div>
                  <Button className="w-full" onClick={() => { setShowMobileCart(false); goToCheckout(); }} disabled={cartItems.length===0}>結帳</Button>
                  <Button variant="outline" className="w-full" onClick={() => setOpenClearCartDialog(true)} disabled={cartItems.length===0}>清空</Button>
                </div>
             </div>
          </div>
        </div>
      )}

      {/* Clear Cart Dialog */}
      <Dialog open={openClearCartDialog} onOpenChange={setOpenClearCartDialog}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>清空購物車</DialogTitle>
            <DialogDescription>
              確定要清空購物車內的所有商品嗎？此操作無法復原。
            </DialogDescription>
          </DialogHeader>
          <DialogFooter>
            <Button variant="outline" onClick={() => setOpenClearCartDialog(false)}>
              取消
            </Button>
            <Button variant="destructive" onClick={clearCart}>
              確定清空
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}

export default Shop;
