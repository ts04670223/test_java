import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import {
  Container,
  Grid,
  Typography,
  Button,
  Card,
  CardMedia,
  Box,
  Chip,
  Rating,
  Divider,
  IconButton,
  TextField,
  Tabs,
  Tab,
  Alert
} from '@mui/material';
import {
  ShoppingCart,
  Favorite,
  FavoriteBorder,
  Share,
  Add,
  Remove
} from '@mui/icons-material';
import { useQuery, useMutation, useQueryClient } from 'react-query';
import toast from 'react-hot-toast';
import api, { cartAPI } from '../services/api';
import { useAuthStore } from '../stores/authStore';

function TabPanel({ children, value, index, ...other }) {
  return (
    <div
      role="tabpanel"
      hidden={value !== index}
      id={`product-tabpanel-${index}`}
      aria-labelledby={`product-tab-${index}`}
      {...other}
    >
      {value === index && (
        <Box sx={{ p: 3 }}>
          {children}
        </Box>
      )}
    </div>
  );
}

export default function ProductDetail() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { user } = useAuthStore();
  const queryClient = useQueryClient();

  const [selectedImage, setSelectedImage] = useState(0);
  const [selectedVariant, setSelectedVariant] = useState(null);
  const [quantity, setQuantity] = useState(1);
  const [tabValue, setTabValue] = useState(0);
  const [reviewText, setReviewText] = useState('');
  const [reviewRating, setReviewRating] = useState(5);
  const [isWishlisted, setIsWishlisted] = useState(false);

  // 獲取產品詳情
  const { data: product, isLoading, error } = useQuery(
    ['product', id],
    () => api.get(`/products/${id}`).then(res => res.data),
    {
      enabled: !!id,
      onSuccess: (data) => {
        if (data.variants && data.variants.length > 0) {
          setSelectedVariant(data.variants[0]);
        }
      }
    }
  );

  // 獲取產品評論
  const { data: reviews } = useQuery(
    ['product-reviews', id],
    () => api.get(`/products/${id}/reviews`).then(res => res.data),
    { enabled: !!id }
  );

  // 添加到購物車
  const addToCartMutation = useMutation(
    async () => {
      if (!user?.id) {
        throw new Error('請先登入');
      }
      return await cartAPI.addToCart(user.id, {
        productId: product.id,
        quantity
      });
    },
    {
      onSuccess: () => {
        toast.success('已添加到購物車');
      },
      onError: (error) => {
        toast.error(error.response?.data?.message || '添加失敗');
      }
    }
  );

  // 添加評論
  const addReviewMutation = useMutation(
    (data) => api.post(`/products/${id}/reviews`, data),
    {
      onSuccess: () => {
        toast.success('評論提交成功');
        setReviewText('');
        setReviewRating(5);
        queryClient.invalidateQueries(['product-reviews', id]);
      },
      onError: (error) => {
        toast.error(error.response?.data?.message || '評論提交失敗');
      }
    }
  );

  // 加入/移除願望清單
  const toggleWishlistMutation = useMutation(
    () => {
      return isWishlisted
        ? api.delete(`/wishlist/items/${id}`)
        : api.post('/wishlist/items', null, { params: { productId: id } });
    },
    {
      onSuccess: () => {
        setIsWishlisted(!isWishlisted);
        toast.success(isWishlisted ? '已從願望清單移除' : '已加入願望清單');
      }
    }
  );

  const handleAddToCart = () => {
    if (!user) {
      navigate('/login');
      return;
    }

    addToCartMutation.mutate({
      productId: product.id,
      variantId: selectedVariant?.id,
      quantity
    });
  };

  const handleSubmitReview = () => {
    if (!user) {
      navigate('/login');
      return;
    }

    if (!reviewText.trim()) {
      toast.error('請輸入評論內容');
      return;
    }

    addReviewMutation.mutate({
      rating: reviewRating,
      comment: reviewText
    });
  };

  const getCurrentPrice = () => {
    return selectedVariant ? selectedVariant.price : product?.price || 0;
  };

  const getStockQuantity = () => {
    return selectedVariant ? selectedVariant.stock : product?.stock || 0;
  };

  if (isLoading) {
    return (
      <Container sx={{ py: 4 }}>
        <Typography>載入中...</Typography>
      </Container>
    );
  }

  if (error || !product) {
    return (
      <Container sx={{ py: 4 }}>
        <Alert severity="error">
          找不到該產品或載入失敗
        </Alert>
      </Container>
    );
  }

  return (
    <Container maxWidth="lg" sx={{ py: 4 }}>
      <Grid container spacing={4}>
        {/* 產品圖片 */}
        <Grid item xs={12} md={6}>
          <Card>
            <CardMedia
              component="img"
              height="400"
              image={product.images?.[selectedImage]?.url || '/api/placeholder/400/400'}
              alt={product.name}
              sx={{ objectFit: 'contain' }}
            />
          </Card>

          {product.images && product.images.length > 1 && (
            <Box sx={{ display: 'flex', gap: 1, mt: 2, overflow: 'auto' }}>
              {product.images.map((image, index) => (
                <Card
                  key={index}
                  sx={{
                    minWidth: 80,
                    cursor: 'pointer',
                    border: selectedImage === index ? '2px solid primary.main' : 'none'
                  }}
                  onClick={() => setSelectedImage(index)}
                >
                  <CardMedia
                    component="img"
                    height="80"
                    image={image.url}
                    alt={`${product.name} ${index + 1}`}
                  />
                </Card>
              ))}
            </Box>
          )}
        </Grid>

        {/* 產品信息 */}
        <Grid item xs={12} md={6}>
          <Typography variant="h4" gutterBottom>
            {product.name}
          </Typography>

          <Box sx={{ display: 'flex', alignItems: 'center', gap: 2, mb: 2 }}>
            <Rating value={product.averageRating || 0} readOnly precision={0.1} />
            <Typography variant="body2" color="text.secondary">
              ({product.reviewCount || 0} 評論)
            </Typography>
          </Box>

          <Typography variant="h5" color="primary" gutterBottom>
            NT$ {getCurrentPrice()}
          </Typography>

          <Typography variant="body1" paragraph>
            {product.description}
          </Typography>

          {/* 產品變體選擇 */}
          {product.variants && product.variants.length > 0 && (
            <Box sx={{ mb: 3 }}>
              <Typography variant="h6" gutterBottom>
                選擇規格
              </Typography>
              <Box sx={{ display: 'flex', gap: 1, flexWrap: 'wrap' }}>
                {product.variants.map((variant) => (
                  <Chip
                    key={variant.id}
                    label={`${variant.name} - NT$ ${variant.price}`}
                    variant={selectedVariant?.id === variant.id ? 'filled' : 'outlined'}
                    color={selectedVariant?.id === variant.id ? 'primary' : 'default'}
                    onClick={() => setSelectedVariant(variant)}
                  />
                ))}
              </Box>
            </Box>
          )}

          {/* 標籤 */}
          {product.tags && product.tags.length > 0 && (
            <Box sx={{ mb: 3 }}>
              <Typography variant="h6" gutterBottom>
                標籤
              </Typography>
              <Box sx={{ display: 'flex', gap: 1, flexWrap: 'wrap' }}>
                {product.tags.map((tag) => (
                  <Chip key={tag.id} label={tag.name} size="small" />
                ))}
              </Box>
            </Box>
          )}

          {/* 數量選擇 */}
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 2, mb: 3 }}>
            <Typography variant="h6">數量：</Typography>
            <Box sx={{ display: 'flex', alignItems: 'center' }}>
              <IconButton
                onClick={() => setQuantity(Math.max(1, quantity - 1))}
                disabled={quantity <= 1}
              >
                <Remove />
              </IconButton>
              <Typography sx={{ mx: 2, minWidth: 40, textAlign: 'center' }}>
                {quantity}
              </Typography>
              <IconButton
                onClick={() => setQuantity(Math.min(getStockQuantity(), quantity + 1))}
                disabled={quantity >= getStockQuantity()}
              >
                <Add />
              </IconButton>
            </Box>
            <Typography variant="body2" color="text.secondary">
              庫存：{getStockQuantity()}
            </Typography>
          </Box>

          {/* 操作按鈕 */}
          <Box sx={{ display: 'flex', gap: 2, mb: 3 }}>
            <Button
              variant="contained"
              size="large"
              startIcon={<ShoppingCart />}
              onClick={handleAddToCart}
              disabled={getStockQuantity() === 0 || addToCartMutation.isLoading}
              sx={{ flex: 1 }}
            >
              {getStockQuantity() === 0 ? '缺貨' : '加入購物車'}
            </Button>

            <IconButton
              onClick={() => toggleWishlistMutation.mutate()}
              color={isWishlisted ? 'error' : 'default'}
            >
              {isWishlisted ? <Favorite /> : <FavoriteBorder />}
            </IconButton>

            <IconButton>
              <Share />
            </IconButton>
          </Box>
        </Grid>
      </Grid>

      {/* 詳細信息標籤頁 */}
      <Box sx={{ mt: 4 }}>
        <Tabs value={tabValue} onChange={(e, newValue) => setTabValue(newValue)}>
          <Tab label="產品詳情" />
          <Tab label={`評論 (${reviews?.length || 0})`} />
          <Tab label="規格" />
        </Tabs>

        <TabPanel value={tabValue} index={0}>
          <Typography variant="body1" style={{ whiteSpace: 'pre-line' }}>
            {product.longDescription || product.description}
          </Typography>
        </TabPanel>

        <TabPanel value={tabValue} index={1}>
          {/* 寫評論 */}
          {user && (
            <Card sx={{ p: 3, mb: 3 }}>
              <Typography variant="h6" gutterBottom>
                寫評論
              </Typography>
              <Box sx={{ mb: 2 }}>
                <Typography component="legend">評分</Typography>
                <Rating
                  value={reviewRating}
                  onChange={(event, newValue) => setReviewRating(newValue)}
                />
              </Box>
              <TextField
                fullWidth
                multiline
                rows={4}
                value={reviewText}
                onChange={(e) => setReviewText(e.target.value)}
                placeholder="分享您的使用心得..."
                sx={{ mb: 2 }}
              />
              <Button
                variant="contained"
                onClick={handleSubmitReview}
                disabled={addReviewMutation.isLoading}
              >
                提交評論
              </Button>
            </Card>
          )}

          {/* 評論列表 */}
          {reviews?.map((review) => (
            <Card key={review.id} sx={{ p: 3, mb: 2 }}>
              <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 2 }}>
                <Typography variant="h6">{review.user.name}</Typography>
                <Typography variant="body2" color="text.secondary">
                  {new Date(review.createdAt).toLocaleDateString()}
                </Typography>
              </Box>
              <Rating value={review.rating} readOnly size="small" sx={{ mb: 1 }} />
              <Typography variant="body1">{review.comment}</Typography>
            </Card>
          ))}

          {!reviews || reviews.length === 0 && (
            <Typography color="text.secondary">
              暫無評論
            </Typography>
          )}
        </TabPanel>

        <TabPanel value={tabValue} index={2}>
          <Grid container spacing={2}>
            <Grid item xs={6}>
              <Typography variant="body2" color="text.secondary">品牌</Typography>
              <Typography variant="body1">{product.brand || '未指定'}</Typography>
            </Grid>
            <Grid item xs={6}>
              <Typography variant="body2" color="text.secondary">型號</Typography>
              <Typography variant="body1">{product.model || '未指定'}</Typography>
            </Grid>
            <Grid item xs={6}>
              <Typography variant="body2" color="text.secondary">重量</Typography>
              <Typography variant="body1">{product.weight || '未指定'}</Typography>
            </Grid>
            <Grid item xs={6}>
              <Typography variant="body2" color="text.secondary">尺寸</Typography>
              <Typography variant="body1">{product.dimensions || '未指定'}</Typography>
            </Grid>
          </Grid>
        </TabPanel>
      </Box>
    </Container>
  );
}