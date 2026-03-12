import React, { useState, useEffect } from 'react';
import { Outlet } from 'react-router-dom';
import {
  AppBar,
  Toolbar,
  Typography,
  IconButton,
  Badge,
  Menu,
  MenuItem,
  Box,
  Container,
  Drawer,
  List,
  ListItem,
  ListItemIcon,
  ListItemText,
  useTheme,
  useMediaQuery,
} from '@mui/material';
import {
  Menu as MenuIcon,
  ShoppingCart,
  AccountCircle,
  Store,
  FavoriteBorder,
  Receipt,
  Person,
  Logout,
  AdminPanelSettings,
  SmartToy,
} from '@mui/icons-material';
import { useNavigate } from 'react-router-dom';
import { useAuthStore } from '../stores/authStore';
import { cartAPI, wishlistAPI } from '../services/api';
import { cartEvents } from '../utils/cartEvents';
import { wishlistEvents } from '../utils/wishlistEvents';

const Layout = () => {
  const navigate = useNavigate();
  const theme = useTheme();
  const isMobile = useMediaQuery(theme.breakpoints.down('md'));

  const { user, logout } = useAuthStore();
  const [cartCount, setCartCount] = useState(0);
  const [wishlistCount, setWishlistCount] = useState(0);

  const [anchorEl, setAnchorEl] = useState(null);
  const [mobileOpen, setMobileOpen] = useState(false);

  useEffect(() => {
    loadCartCount();
    loadWishlistCount();

    // 訂閱購物車變更事件
    const unsubscribeCart = cartEvents.subscribe(() => {
      loadCartCount();
    });

    // 訂閱願望清單變更事件
    const unsubscribeWishlist = wishlistEvents.subscribe(() => {
      loadWishlistCount();
    });

    // 每10秒更新一次購物車數量
    const interval = setInterval(() => {
      loadCartCount();
      loadWishlistCount();
    }, 10000);

    return () => {
      clearInterval(interval);
      unsubscribeCart();
      unsubscribeWishlist();
    };
  }, [user?.id]);

  const loadCartCount = async () => {
    if (!user?.id) return;

    try {
      const response = await cartAPI.getCart(user.id);
      const cartData = response.data?.data || response.data;
      const count = cartData?.items?.reduce((sum, item) => sum + (item.quantity || 0), 0) || 0;
      setCartCount(count);
    } catch (error) {
      console.error('載入購物車數量失敗:', error);
    }
  };

  const loadWishlistCount = async () => {
    if (!user?.id) return;

    try {
      const response = await wishlistAPI.getWishlist();
      const wishlistData = response.data?.data || response.data;
      const count = Array.isArray(wishlistData) ? wishlistData.length : (wishlistData?.items?.length || 0);
      setWishlistCount(count);
    } catch (error) {
      console.error('載入願望清單數量失敗:', error);
    }
  };

  const handleMenu = (event) => {
    setAnchorEl(event.currentTarget);
  };

  const handleClose = () => {
    setAnchorEl(null);
  };

  const handleLogout = () => {
    logout();
    navigate('/login');
    handleClose();
  };

  const handleDrawerToggle = () => {
    setMobileOpen(!mobileOpen);
  };

  const menuItems = [
    { text: '商店', icon: <Store />, path: '/shop' },
    // { text: 'AI助手', icon: <SmartToy />, path: '/ai' }, // TODO: AI 功能暫時停用
    { text: '購物車', icon: <ShoppingCart />, path: '/cart' },
    { text: '願望清單', icon: <FavoriteBorder />, path: '/wishlist' },
    { text: '訂單', icon: <Receipt />, path: '/orders' },
  ];

  const drawer = (
    <Box sx={{ width: 250 }}>
      <List>
        {menuItems.map((item) => (
          <ListItem
            key={item.text}
            component="button"
            onClick={() => {
              navigate(item.path);
              setMobileOpen(false);
            }}
            sx={{
              cursor: 'pointer',
              '&:hover': { bgcolor: 'action.hover' }
            }}
          >
            <ListItemIcon>{item.icon}</ListItemIcon>
            <ListItemText primary={item.text} />
          </ListItem>
        ))}
      </List>
    </Box>
  );

  return (
    <Box sx={{ display: 'flex' }}>
      <AppBar position="fixed">
        <Toolbar>
          {isMobile && (
            <IconButton
              color="inherit"
              aria-label="open drawer"
              edge="start"
              onClick={handleDrawerToggle}
              sx={{ mr: 2 }}
            >
              <MenuIcon />
            </IconButton>
          )}

          <Typography
            variant="h6"
            component="div"
            sx={{ flexGrow: 1, cursor: 'pointer' }}
            onClick={() => navigate('/shop')}
          >
            現代電商平台
          </Typography>

          {!isMobile && (
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
              {menuItems.map((item) => (
                <IconButton
                  key={item.text}
                  color="inherit"
                  onClick={() => navigate(item.path)}
                  title={item.text}
                >
                  {item.text === '購物車' ? (
                    <Badge badgeContent={cartCount} color="secondary">
                      {item.icon}
                    </Badge>
                  ) : item.text === '願望清單' ? (
                    <Badge badgeContent={wishlistCount} color="error">
                      {item.icon}
                    </Badge>
                  ) : (
                    item.icon
                  )}
                </IconButton>
              ))}
            </Box>
          )}

          <IconButton
            size="large"
            aria-label="account of current user"
            aria-controls="menu-appbar"
            aria-haspopup="true"
            onClick={handleMenu}
            color="inherit"
          >
            <AccountCircle />
          </IconButton>

          <Menu
            id="menu-appbar"
            anchorEl={anchorEl}
            anchorOrigin={{
              vertical: 'top',
              horizontal: 'right',
            }}
            keepMounted
            transformOrigin={{
              vertical: 'top',
              horizontal: 'right',
            }}
            open={Boolean(anchorEl)}
            onClose={handleClose}
          >
            <MenuItem onClick={() => { navigate('/profile'); handleClose(); }}>
              <Person sx={{ mr: 1 }} />
              個人資料
            </MenuItem>

            {user?.role === 'ADMIN' && (
              <MenuItem onClick={() => { navigate('/admin'); handleClose(); }}>
                <AdminPanelSettings sx={{ mr: 1 }} />
                管理後台
              </MenuItem>
            )}

            <MenuItem onClick={handleLogout}>
              <Logout sx={{ mr: 1 }} />
              登出
            </MenuItem>
          </Menu>
        </Toolbar>
      </AppBar>

      {isMobile && (
        <Drawer
          variant="temporary"
          open={mobileOpen}
          onClose={handleDrawerToggle}
          ModalProps={{
            keepMounted: true,
          }}
        >
          {drawer}
        </Drawer>
      )}

      <Box
        component="main"
        sx={{
          flexGrow: 1,
          pt: 8,
          minHeight: '100vh',
          bgcolor: 'background.default',
        }}
      >
        <Container maxWidth="xl">
          <Outlet />
        </Container>
      </Box>
    </Box>
  );
};

export default Layout;