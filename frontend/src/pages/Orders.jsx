import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { toast } from 'react-hot-toast';
import { orderAPI } from '../services/api';
import { useAuthStore } from '../stores/authStore';

import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
} from '@/components/ui/dialog';
import {
    Loader2,
    ShoppingBag,
    MessageCircle,
    LogOut,
    Package,
    Calendar,
    MapPin,
    Phone,
    CreditCard,
    Info
} from 'lucide-react';

function Orders() {
  const { user, logout } = useAuthStore();
  const navigate = useNavigate();
  const [orders, setOrders] = useState([]);
  const [loading, setLoading] = useState(true);
  const [selectedOrder, setSelectedOrder] = useState(null);
  const [isDialogOpen, setIsDialogOpen] = useState(false);

  useEffect(() => {
    if (!user?.id) {
      toast.error('請先登入');
      navigate('/login');
      return;
    }
    loadOrders();
  }, [user?.id]);

  const loadOrders = async () => {
    if (!user?.id) return;

    try {
      const response = await orderAPI.getUserOrders(user.id);
      const ordersData = response.data?.data || response.data || [];
      setOrders(Array.isArray(ordersData) ? ordersData : []);
      setLoading(false);
    } catch (error) {
      console.error('載入訂單失敗:', error);
      toast.error('載入訂單失敗');
      setOrders([]);
      setLoading(false);
    }
  };

  const loadOrderDetail = async (orderId) => {
    try {
      const response = await orderAPI.getOrder(orderId);
      const orderData = response.data?.data || response.data;
      setSelectedOrder(orderData);
      setIsDialogOpen(true);
    } catch (error) {
      console.error('載入訂單詳情失敗:', error);
      toast.error('載入訂單詳情失敗');
    }
  };

  const getStatusBadge = (status) => {
    const statusMap = {
      'PENDING': { text: '待處理', variant: 'secondary' },
      'PROCESSING': { text: '處理中', variant: 'default' },
      'SHIPPED': { text: '已發貨', variant: 'default' }, // Maybe custom color?
      'DELIVERED': { text: '已送達', variant: 'outline' }, // Green-ish usually but outline works
      'CANCELLED': { text: '已取消', variant: 'destructive' },
      'REFUNDED': { text: '已退款', variant: 'destructive' }
    };
    const info = statusMap[status] || { text: status, variant: 'outline' };
    return <Badge variant={info.variant}>{info.text}</Badge>;
  };

  if (loading) {
     return (
       <div className='container mx-auto py-8 text-center flex justify-center'>
         <Loader2 className='h-8 w-8 animate-spin' />
       </div>
     );
  }

  return (
    <div className='container mx-auto py-8 px-4'>
      <div className='flex flex-col md:flex-row justify-between items-center mb-8 gap-4'>
        <h1 className='text-3xl font-bold flex items-center'>
            <Package className='mr-2 h-8 w-8' /> 我的訂單
        </h1>
        
        <div className='flex flex-wrap gap-2'>
            <Button variant='outline' onClick={() => navigate('/shop')}>
                <ShoppingBag className='mr-2 h-4 w-4' /> 繼續購物
            </Button>
            <Button variant='outline' onClick={() => navigate('/chat')}>
                <MessageCircle className='mr-2 h-4 w-4' /> 聊天室
            </Button>
             <p className='flex items-center px-4 font-medium'>
                 {user?.name || user?.username || '用戶'}
            </p>
            <Button variant='destructive' onClick={() => { logout(); navigate('/login'); }}>
                <LogOut className='mr-2 h-4 w-4' /> 登出
            </Button>
        </div>
      </div>

      {orders.length === 0 ? (
        <div className='text-center py-12'>
            <Package className='h-16 w-16 mx-auto text-gray-300 mb-4' />
            <h2 className='text-xl font-semibold mb-2'>還沒有訂單</h2>
            <p className='text-gray-500 mb-6'>您還沒有購買任何商品</p>
            <Button onClick={() => navigate('/shop')}>
                開始購物
            </Button>
        </div>
      ) : (
        <div className='grid gap-6 md:grid-cols-2 lg:grid-cols-3'>
            {orders.map(order => (
              <Card 
                key={order.id} 
                className='cursor-pointer hover:shadow-lg transition-shadow'
                onClick={() => loadOrderDetail(order.id)}
              >
                <CardHeader className='pb-2'>
                    <div className='flex justify-between items-start'>
                        <div>
                            <CardTitle className='text-lg'>訂單 #{order.orderNumber}</CardTitle>
                            <CardDescription className='flex items-center mt-1'>
                                <Calendar className='h-3 w-3 mr-1' />
                                {new Date(order.createdAt).toLocaleString('zh-TW')}
                            </CardDescription>
                        </div>
                        {getStatusBadge(order.status)}
                    </div>
                </CardHeader>
                <CardContent>
                    <div className='space-y-2 text-sm'>
                        <div className='flex items-start'>
                            <MapPin className='h-4 w-4 mr-2 text-muted-foreground mt-0.5' />
                            <span className='line-clamp-2'>{order.shippingAddress}</span>
                        </div>
                        <div className='flex items-center'>
                            <Phone className='h-4 w-4 mr-2 text-muted-foreground' />
                            <span>{order.phone}</span>
                        </div>
                         <div className='flex justify-between items-center mt-4 pt-4 border-t'>
                            <span className='font-medium'>總金額</span>
                            <span className='text-lg font-bold text-primary'>${order.totalAmount}</span>
                        </div>
                    </div>
                </CardContent>
              </Card>
            ))}
        </div>
      )}

      <Dialog open={isDialogOpen} onOpenChange={setIsDialogOpen}>
        <DialogContent className='max-w-2xl max-h-[90vh] overflow-y-auto'>
            <DialogHeader>
                <DialogTitle>訂單詳情</DialogTitle>
                <DialogDescription>
                    訂單編號: {selectedOrder?.orderNumber}
                </DialogDescription>
            </DialogHeader>

            {selectedOrder && (
                <div className='space-y-6'>
                    <div className='flex flex-wrap gap-4 p-4 bg-muted/50 rounded-lg'>
                        <div className='flex-1 min-w-[200px]'>
                            <h4 className='text-sm font-medium mb-2 text-muted-foreground'>狀態</h4>
                            <div>{getStatusBadge(selectedOrder.status)}</div>
                        </div>
                         <div className='flex-1 min-w-[200px]'>
                            <h4 className='text-sm font-medium mb-2 text-muted-foreground'>下單時間</h4>
                            <div className='text-sm font-medium'>
                                {new Date(selectedOrder.createdAt).toLocaleString('zh-TW')}
                            </div>
                        </div>
                    </div>

                    <div className='grid gap-4 md:grid-cols-2'>
                        <div className='space-y-2'>
                            <h4 className='font-medium flex items-center'>
                                <MapPin className='h-4 w-4 mr-2' /> 收貨資訊
                            </h4>
                            <div className='p-3 border rounded-md text-sm space-y-1'>
                                <p>{selectedOrder.shippingAddress}</p>
                                <p className='text-muted-foreground'>{selectedOrder.phone}</p>
                            </div>
                        </div>
                         <div className='space-y-2'>
                            <h4 className='font-medium flex items-center'>
                                <Info className='h-4 w-4 mr-2' /> 備註
                            </h4>
                            <div className='p-3 border rounded-md text-sm min-h-[80px]'>
                                {selectedOrder.note || '無備註'}
                            </div>
                        </div>
                    </div>

                    <div>
                        <h4 className='font-medium mb-3'>訂單項目</h4>
                        <div className='space-y-2'>
                             {selectedOrder.items?.map(item => (
                                <div key={item.id} className='flex justify-between items-center p-3 bg-card border rounded-md'>
                                    <div>
                                        <p className='font-medium'>{item.productName}</p>
                                        <p className='text-sm text-muted-foreground'>
                                            ${item.price} × {item.quantity}
                                        </p>
                                    </div>
                                    <p className='font-bold'>
                                        ${(item.price * item.quantity).toFixed(0)}
                                    </p>
                                </div>
                            ))}
                        </div>
                    </div>

                    <div className='flex justify-between items-center pt-4 border-t'>
                        <span className='font-bold text-lg'>總計</span>
                        <span className='font-bold text-2xl text-primary'>${selectedOrder.totalAmount}</span>
                    </div>
                </div>
            )}
        </DialogContent>
      </Dialog>
    </div>
  );
}

export default Orders;
