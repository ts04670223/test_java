import React from 'react';
import {
  Container,
  Typography,
  Grid,
  Card,
  CardContent,
  Button,
  Box,
  Avatar,
  Divider
} from '@mui/material';
import {
  Person,
  Email,
  Phone,
  Edit,
  Security,
  Notifications,
  Key
} from '@mui/icons-material';
import { useNavigate } from 'react-router-dom';
import { useAuthStore } from '../stores/authStore';

export default function Profile() {
  const { user } = useAuthStore();
  const navigate = useNavigate();

  if (!user) {
    return (
      <Container maxWidth="md" sx={{ py: 4, textAlign: 'center' }}>
        <Typography variant="h4">請先登入</Typography>
      </Container>
    );
  }

  return (
    <Container maxWidth="lg" sx={{ py: 4 }}>
      <Typography variant="h4" gutterBottom>
        個人資料
      </Typography>

      <Grid container spacing={4}>
        {/* 基本資料 */}
        <Grid size={{ xs: 12, md: 8 }}>
          <Card>
            <CardContent>
              <Box sx={{ display: 'flex', alignItems: 'center', mb: 3 }}>
                <Avatar
                  sx={{ width: 80, height: 80, mr: 3 }}
                  src={user.avatar}
                >
                  {user.name?.charAt(0)}
                </Avatar>
                <Box>
                  <Typography variant="h5">{user.name}</Typography>
                  <Typography variant="body2" color="text.secondary">
                    會員等級: {user.memberLevel || '一般會員'}
                  </Typography>
                </Box>
              </Box>

              <Divider sx={{ my: 3 }} />

              <Grid container spacing={3}>
                <Grid size={{ xs: 12, sm: 6 }}>
                  <Box sx={{ display: 'flex', alignItems: 'center', mb: 2 }}>
                    <Person sx={{ mr: 2, color: 'text.secondary' }} />
                    <Box>
                      <Typography variant="body2" color="text.secondary">
                        姓名
                      </Typography>
                      <Typography variant="body1">{user.name}</Typography>
                    </Box>
                  </Box>
                </Grid>

                <Grid size={{ xs: 12, sm: 6 }}>
                  <Box sx={{ display: 'flex', alignItems: 'center', mb: 2 }}>
                    <Email sx={{ mr: 2, color: 'text.secondary' }} />
                    <Box>
                      <Typography variant="body2" color="text.secondary">
                        電子郵件
                      </Typography>
                      <Typography variant="body1">{user.email}</Typography>
                    </Box>
                  </Box>
                </Grid>

                <Grid size={{ xs: 12, sm: 6 }}>
                  <Box sx={{ display: 'flex', alignItems: 'center', mb: 2 }}>
                    <Phone sx={{ mr: 2, color: 'text.secondary' }} />
                    <Box>
                      <Typography variant="body2" color="text.secondary">
                        手機號碼
                      </Typography>
                      <Typography variant="body1">{user.mobile || '未設定'}</Typography>
                    </Box>
                  </Box>
                </Grid>

                <Grid size={{ xs: 12, sm: 6 }}>
                  <Box sx={{ display: 'flex', alignItems: 'center', mb: 2 }}>
                    <Box>
                      <Typography variant="body2" color="text.secondary">
                        註冊日期
                      </Typography>
                      <Typography variant="body1">
                        {user.createdAt ? new Date(user.createdAt).toLocaleDateString() : '未知'}
                      </Typography>
                    </Box>
                  </Box>
                </Grid>
              </Grid>

              <Box sx={{ mt: 3 }}>
                <Button
                  variant="contained"
                  startIcon={<Edit />}
                  sx={{ mr: 2 }}
                >
                  編輯資料
                </Button>
                <Button
                  variant="outlined"
                  startIcon={<Security />}
                >
                  更改密碼
                </Button>
              </Box>
            </CardContent>
          </Card>
        </Grid>

        {/* 快速操作 */}
        <Grid size={{ xs: 12, md: 4 }}>
          <Card sx={{ mb: 3 }}>
            <CardContent>
              <Typography variant="h6" gutterBottom>
                快速操作
              </Typography>

              <Button
                fullWidth
                variant="outlined"
                startIcon={<Notifications />}
                sx={{ mb: 2 }}
              >
                通知設定
              </Button>

              <Button
                fullWidth
                variant="outlined"
                sx={{ mb: 2 }}
              >
                地址管理
              </Button>

              <Button
                fullWidth
                variant="outlined"
                sx={{ mb: 2 }}
              >
                付款方式
              </Button>

              <Button
                fullWidth
                variant="outlined"
                color="primary"
                startIcon={<Key />}
                onClick={() => navigate('/passkeys')}
              >
                Passkey 管理（無密碼驗證）
              </Button>
            </CardContent>
          </Card>

          {/* 統計信息 */}
          <Card>
            <CardContent>
              <Typography variant="h6" gutterBottom>
                我的統計
              </Typography>

              <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 2 }}>
                <Typography variant="body2">總訂單數</Typography>
                <Typography variant="body1" color="primary">
                  {user.orderCount || 0}
                </Typography>
              </Box>

              <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 2 }}>
                <Typography variant="body2">累計消費</Typography>
                <Typography variant="body1" color="primary">
                  NT$ {user.totalSpent || 0}
                </Typography>
              </Box>

              <Box sx={{ display: 'flex', justifyContent: 'space-between' }}>
                <Typography variant="body2">會員積分</Typography>
                <Typography variant="body1" color="primary">
                  {user.points || 0}
                </Typography>
              </Box>
            </CardContent>
          </Card>
        </Grid>
      </Grid>
    </Container>
  );
}