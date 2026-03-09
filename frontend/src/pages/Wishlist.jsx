import React, { useState } from 'react';
import {
  Container,
  Typography,
  Grid,
  Card,
  CardContent,
  CardMedia,
  Button,
  Box,
  IconButton,
  Chip
} from '@mui/material';
import {
  Delete,
  ShoppingCart,
  Favorite
} from '@mui/icons-material';
import { useNavigate } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from 'react-query';
import toast from 'react-hot-toast';
import api, { cartAPI } from '../services/api';
import { useAuthStore } from '../stores/authStore';

export default function Wishlist() {
  const navigate = useNavigate();
  const { user } = useAuthStore();
  const queryClient = useQueryClient();

  // 獲取願望清單
  const { data: wishlistItems, isLoading } = useQuery(
    'wishlist',
    () => api.get('/wishlist').then(res => {
      const items = res.data?.items ?? res.data ?? [];
      return items.map(item => ({
        id: item.id,
        createdAt: item.createdAt,
        product: {
          id: item.productId,
          name: item.productName,
          price: item.productPrice,
          description: item.productDescription || '',
          tags: [],
          images: (item.productImages || []).map(url => ({ url })),
        }
      }));
    }),
    {
      onError: (error) => {
        if (error.response?.status === 401) {
          navigate('/login');
        }
      }
    }
  );

  // 從願望清單移除
  const removeFromWishlistMutation = useMutation(
    (productId) => api.delete(`/wishlist/items/${productId}`),
    {
      onSuccess: () => {
        toast.success('已從願望清單移除');
        queryClient.invalidateQueries('wishlist');
      },
      onError: (error) => {
        toast.error(error.response?.data?.message || '移除失敗');
      }
    }
  );

  // 添加到購物車
  const addToCartMutation = useMutation(
    async (productId) => {
      if (!user?.id) {
        throw new Error('請先登入');
      }
      return await cartAPI.addToCart(user.id, {
        productId,
        quantity: 1
      });
    },
    {
      onSuccess: () => {
        toast.success('已添加到購物車');
      },
      onError: (error) => {
        toast.error(error.response?.data?.error || error.message || '添加失敗');
      }
    }
  );

  const handleRemoveFromWishlist = (productId) => {
    removeFromWishlistMutation.mutate(productId);
  };

  const handleAddToCart = (productId) => {
    addToCartMutation.mutate(productId);
  };

  const handleViewProduct = (productId) => {
    navigate(`/product/${productId}`);
  };

  if (isLoading) {
    return (
      <Container maxWidth="lg" sx={{ py: 4 }}>
        <Typography>載入中...</Typography>
      </Container>
    );
  }

  if (!wishlistItems || wishlistItems.length === 0) {
    return (
      <Container maxWidth="md" sx={{ py: 4, textAlign: 'center' }}>
        <Favorite sx={{ fontSize: 64, color: 'text.secondary', mb: 2 }} />
        <Typography variant="h4" gutterBottom>
          願望清單是空的
        </Typography>
        <Typography variant="body1" color="text.secondary" paragraph>
          將您喜歡的商品加入願望清單，方便以後購買
        </Typography>
        <Button
          variant="contained"
          size="large"
          onClick={() => navigate('/shop')}
        >
          去購物
        </Button>
      </Container>
    );
  }

  return (
    <Container maxWidth="lg" sx={{ py: 4 }}>
      <Box sx={{ display: 'flex', alignItems: 'center', mb: 4 }}>
        <Favorite sx={{ mr: 2, color: 'primary.main' }} />
        <Typography variant="h4">
          我的願望清單 ({wishlistItems.length})
        </Typography>
      </Box>

      <Grid container spacing={3}>
        {wishlistItems.map((item) => (
          <Grid item xs={12} sm={6} md={4} lg={3} key={item.id}>
            <Card sx={{ height: '100%', display: 'flex', flexDirection: 'column' }}>
              <Box sx={{ position: 'relative' }}>
                <CardMedia
                  component="img"
                  height="200"
                  image={item.product.images?.[0]?.url || '/api/placeholder/200/200'}
                  alt={item.product.name}
                  sx={{
                    objectFit: 'contain',
                    cursor: 'pointer'
                  }}
                  onClick={() => handleViewProduct(item.product.id)}
                />
                <IconButton
                  sx={{
                    position: 'absolute',
                    top: 8,
                    right: 8,
                    backgroundColor: 'rgba(255, 255, 255, 0.9)',
                    '&:hover': {
                      backgroundColor: 'rgba(255, 255, 255, 1)',
                    }
                  }}
                  onClick={() => handleRemoveFromWishlist(item.product.id)}
                  color="error"
                >
                  <Delete />
                </IconButton>
              </Box>

              <CardContent sx={{ flexGrow: 1, display: 'flex', flexDirection: 'column' }}>
                <Typography
                  variant="h6"
                  gutterBottom
                  sx={{
                    cursor: 'pointer',
                    '&:hover': { color: 'primary.main' }
                  }}
                  onClick={() => handleViewProduct(item.product.id)}
                >
                  {item.product.name}
                </Typography>

                <Typography
                  variant="body2"
                  color="text.secondary"
                  paragraph
                  sx={{
                    overflow: 'hidden',
                    textOverflow: 'ellipsis',
                    display: '-webkit-box',
                    WebkitLineClamp: 2,
                    WebkitBoxOrient: 'vertical',
                  }}
                >
                  {item.product.description}
                </Typography>

                {/* 標籤 */}
                {item.product.tags && item.product.tags.length > 0 && (
                  <Box sx={{ mb: 2 }}>
                    {item.product.tags.slice(0, 2).map((tag) => (
                      <Chip
                        key={tag.id}
                        label={tag.name}
                        size="small"
                        sx={{ mr: 0.5, mb: 0.5 }}
                      />
                    ))}
                  </Box>
                )}

                <Box sx={{ mt: 'auto' }}>
                  <Typography variant="h6" color="primary" gutterBottom>
                    NT$ {item.product.price}
                  </Typography>

                  <Box sx={{ display: 'flex', gap: 1 }}>
                    <Button
                      variant="contained"
                      size="small"
                      startIcon={<ShoppingCart />}
                      onClick={() => handleAddToCart(item.product.id)}
                      disabled={addToCartMutation.isLoading}
                      sx={{ flex: 1 }}
                    >
                      加入購物車
                    </Button>

                    <Button
                      variant="outlined"
                      size="small"
                      onClick={() => handleViewProduct(item.product.id)}
                    >
                      查看
                    </Button>
                  </Box>
                </Box>
              </CardContent>
            </Card>
          </Grid>
        ))}
      </Grid>
    </Container>
  );
}