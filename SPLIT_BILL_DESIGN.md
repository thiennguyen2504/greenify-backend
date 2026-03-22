# 🧾 Split Bill - Ứng dụng Chia Tiền Nhóm

## 📋 Tổng Quan Dự Án

**Split Bill** là ứng dụng web giúp nhóm bạn bè quản lý chi tiêu và chia tiền cho các chuyến đi chơi một cách đơn giản, minh bạch và hiệu quả.

### 🎯 Mục Tiêu
- Tạo và quản lý các chuyến đi chơi
- Theo dõi chi tiêu theo thời gian thực
- Tính toán tự động ai nợ ai bao nhiêu với thuật toán tối ưu
- Hỗ trợ thanh toán với QR code và minh chứng
- Chia sẻ kinh nghiệm qua public trips

---

## 🏗️ Kiến Trúc Hệ Thống

### Tech Stack

#### Backend
- **Framework**: Spring Boot 3.x (Java 17+)
- **Database**: PostgreSQL 15+
- **Cache**: Caffeine Cache (in-memory)
- **Security**: Spring Security + JWT
- **File Storage**: Cloudinary / MinIO
- **Email**: Spring Mail / SendGrid
- **Social Auth**: OAuth2 (Google, Facebook)

#### Frontend
- **Framework**: React 18+ (Vite)
- **UI Library**: Shadcn UI (Radix UI + Tailwind CSS)
- **State Management**: React Query + Zustand
- **Routing**: React Router v6
- **HTTP Client**: Axios
- **Forms**: React Hook Form + Zod

#### DevOps
- **Development**: Docker Compose (PostgreSQL, MinIO)
- **Frontend Deploy**: Vercel
- **Backend Deploy**: GCP Cloud Run / VPS (Docker)
- **CI/CD**: GitHub Actions

---

## 🗄️ Database Schema

### Core Entities

```sql
-- Users Table
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255), -- nullable for social login
    full_name VARCHAR(255) NOT NULL,
    avatar_url VARCHAR(500),
    auth_provider VARCHAR(50) DEFAULT 'LOCAL', -- LOCAL, GOOGLE, FACEBOOK
    provider_id VARCHAR(255), -- ID from OAuth provider
    email_verified BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- QR Codes (Reusable)
CREATE TABLE qr_codes (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(id) ON DELETE CASCADE,
    bank_name VARCHAR(100) NOT NULL,
    account_number VARCHAR(50) NOT NULL,
    account_holder VARCHAR(255) NOT NULL,
    qr_image_url VARCHAR(500) NOT NULL, -- Cloudinary/MinIO URL
    is_default BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Trips Table
CREATE TABLE trips (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    creator_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    budget DECIMAL(15,2), -- Total budget (VND)
    start_date DATE,
    end_date DATE,
    invite_code VARCHAR(50) UNIQUE NOT NULL, -- Share link code
    qr_code_id BIGINT REFERENCES qr_codes(id) ON DELETE SET NULL,
    is_public BOOLEAN DEFAULT FALSE,
    status VARCHAR(20) DEFAULT 'ACTIVE', -- ACTIVE, SETTLED, ARCHIVED
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Trip Members
CREATE TABLE trip_members (
    id BIGSERIAL PRIMARY KEY,
    trip_id BIGINT REFERENCES trips(id) ON DELETE CASCADE,
    user_id BIGINT REFERENCES users(id) ON DELETE CASCADE,
    nickname VARCHAR(100) NOT NULL, -- For guest users
    role VARCHAR(20) DEFAULT 'MEMBER', -- CREATOR, MEMBER
    join_status VARCHAR(20) DEFAULT 'PENDING', -- PENDING, ACCEPTED, LEFT
    joined_at TIMESTAMP,
    UNIQUE(trip_id, user_id)
);

-- Expenses (Khoản Chi)
CREATE TABLE expenses (
    id BIGSERIAL PRIMARY KEY,
    trip_id BIGINT REFERENCES trips(id) ON DELETE CASCADE,
    paid_by_member_id BIGINT REFERENCES trip_members(id) ON DELETE CASCADE,
    amount DECIMAL(15,2) NOT NULL,
    description VARCHAR(500) NOT NULL,
    category VARCHAR(50), -- FOOD, TRANSPORT, ACCOMMODATION, OTHER (for future)
    expense_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    receipt_url VARCHAR(500), -- Optional receipt image
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Expense Splits (Who owes what)
CREATE TABLE expense_splits (
    id BIGSERIAL PRIMARY KEY,
    expense_id BIGINT REFERENCES expenses(id) ON DELETE CASCADE,
    member_id BIGINT REFERENCES trip_members(id) ON DELETE CASCADE,
    split_amount DECIMAL(15,2) NOT NULL, -- Equal split for now
    UNIQUE(expense_id, member_id)
);

-- Settlements (Thanh toán)
CREATE TABLE settlements (
    id BIGSERIAL PRIMARY KEY,
    trip_id BIGINT REFERENCES trips(id) ON DELETE CASCADE,
    from_member_id BIGINT REFERENCES trip_members(id) ON DELETE CASCADE,
    to_member_id BIGINT REFERENCES trip_members(id) ON DELETE CASCADE,
    amount DECIMAL(15,2) NOT NULL,
    proof_image_url VARCHAR(500), -- Screenshot chuyển khoản
    status VARCHAR(20) DEFAULT 'PENDING', -- PENDING, PAID, CONFIRMED
    paid_at TIMESTAMP,
    confirmed_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Trip Likes/Votes (for public trips)
CREATE TABLE trip_votes (
    id BIGSERIAL PRIMARY KEY,
    trip_id BIGINT REFERENCES trips(id) ON DELETE CASCADE,
    user_id BIGINT REFERENCES users(id) ON DELETE CASCADE,
    vote_type VARCHAR(20) DEFAULT 'LIKE', -- LIKE for now
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(trip_id, user_id)
);

-- Notifications
CREATE TABLE notifications (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(id) ON DELETE CASCADE,
    trip_id BIGINT REFERENCES trips(id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    type VARCHAR(50), -- PAYMENT_REMINDER, TRIP_INVITE, EXPENSE_ADDED, etc.
    is_read BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Indexes
CREATE INDEX idx_trips_creator ON trips(creator_id);
CREATE INDEX idx_trips_invite_code ON trips(invite_code);
CREATE INDEX idx_trip_members_trip ON trip_members(trip_id);
CREATE INDEX idx_trip_members_user ON trip_members(user_id);
CREATE INDEX idx_expenses_trip ON expenses(trip_id);
CREATE INDEX idx_settlements_trip ON settlements(trip_id);
CREATE INDEX idx_notifications_user ON notifications(user_id);
```

---

## 🔌 API Design

### Authentication APIs

```
POST   /api/auth/register                  # Register with email/password
POST   /api/auth/login                     # Login
POST   /api/auth/logout                    # Logout
POST   /api/auth/refresh                   # Refresh token
POST   /api/auth/forgot-password           # Request password reset
POST   /api/auth/reset-password            # Reset password
GET    /api/auth/verify-email/{token}      # Verify email
GET    /api/auth/oauth2/google             # Google OAuth
GET    /api/auth/oauth2/facebook           # Facebook OAuth
GET    /api/auth/me                        # Get current user
```

### Trip APIs

```
# Guest + Authenticated
POST   /api/trips                          # Create trip
GET    /api/trips/{id}                     # Get trip details
PUT    /api/trips/{id}                     # Update trip (creator only)
DELETE /api/trips/{id}                     # Delete trip (creator only)
POST   /api/trips/{id}/join/{inviteCode}   # Join trip via invite link
PUT    /api/trips/{id}/members/{memberId}/accept  # Accept join request
DELETE /api/trips/{id}/members/{memberId}  # Remove member (creator only)
PUT    /api/trips/{id}/budget              # Update budget (creator only)

# Authenticated only
GET    /api/trips                          # Get user's trips
POST   /api/trips/{id}/publish             # Make trip public
DELETE /api/trips/{id}/publish             # Make trip private

# Public trips
GET    /api/trips/public                   # Get all public trips
GET    /api/trips/public/trending          # Get trending trips
POST   /api/trips/{id}/vote                # Like/vote trip
DELETE /api/trips/{id}/vote                # Unlike trip
```

### Expense APIs

```
POST   /api/trips/{tripId}/expenses        # Add expense
GET    /api/trips/{tripId}/expenses        # Get all expenses
PUT    /api/expenses/{id}                  # Update expense
DELETE /api/expenses/{id}                  # Delete expense
GET    /api/trips/{tripId}/balance         # Calculate balances
```

### Settlement APIs

```
GET    /api/trips/{tripId}/settlements     # Get optimal settlements
POST   /api/settlements/{id}/upload-proof  # Upload payment proof
PUT    /api/settlements/{id}/confirm       # Confirm received (receiver only)
```

### QR Code APIs

```
POST   /api/qr-codes                       # Upload QR code
GET    /api/qr-codes                       # Get user's QR codes
PUT    /api/qr-codes/{id}                  # Update QR
DELETE /api/qr-codes/{id}                  # Delete QR
PUT    /api/qr-codes/{id}/set-default      # Set as default
POST   /api/trips/{tripId}/qr-codes        # Assign QR to trip
```

### File Upload APIs

```
POST   /api/upload/image                   # Upload image (QR, proof, receipt)
DELETE /api/upload/image                   # Delete image
```

### Notification APIs

```
GET    /api/notifications                  # Get user notifications
PUT    /api/notifications/{id}/read        # Mark as read
PUT    /api/notifications/read-all         # Mark all as read
```

### Admin APIs

```
GET    /api/admin/stats                    # Dashboard statistics
GET    /api/admin/trips                    # All trips (paginated)
GET    /api/admin/users                    # All users (paginated)
```

---

## 🧮 Settlement Algorithm (Debt Simplification)

### Mục tiêu
Tối ưu hóa số lượng giao dịch thanh toán giữa các thành viên.

### Thuật toán

```java
/**
 * Greedy Algorithm for Debt Simplification
 * Time Complexity: O(n^2)
 * Space Complexity: O(n)
 */
public List<Settlement> calculateOptimalSettlements(Long tripId) {
    // Step 1: Calculate net balance for each member
    Map<Long, BigDecimal> balances = calculateNetBalances(tripId);
    
    // Step 2: Separate creditors (positive) and debtors (negative)
    List<Balance> creditors = new ArrayList<>();
    List<Balance> debtors = new ArrayList<>();
    
    balances.forEach((memberId, amount) -> {
        if (amount.compareTo(BigDecimal.ZERO) > 0) {
            creditors.add(new Balance(memberId, amount));
        } else if (amount.compareTo(BigDecimal.ZERO) < 0) {
            debtors.add(new Balance(memberId, amount.abs()));
        }
    });
    
    // Step 3: Greedy matching
    List<Settlement> settlements = new ArrayList<>();
    int i = 0, j = 0;
    
    while (i < creditors.size() && j < debtors.size()) {
        Balance creditor = creditors.get(i);
        Balance debtor = debtors.get(j);
        
        BigDecimal minAmount = creditor.amount.min(debtor.amount);
        
        settlements.add(Settlement.builder()
            .fromMemberId(debtor.memberId)
            .toMemberId(creditor.memberId)
            .amount(minAmount)
            .status(SettlementStatus.PENDING)
            .build());
        
        creditor.amount = creditor.amount.subtract(minAmount);
        debtor.amount = debtor.amount.subtract(minAmount);
        
        if (creditor.amount.compareTo(BigDecimal.ZERO) == 0) i++;
        if (debtor.amount.compareTo(BigDecimal.ZERO) == 0) j++;
    }
    
    return settlements;
}

private Map<Long, BigDecimal> calculateNetBalances(Long tripId) {
    // Balance = Total Paid - Total Owed
    // Positive = người khác nợ mình (receiver)
    // Negative = mình nợ người khác (payer)
    Map<Long, BigDecimal> balances = new HashMap<>();
    
    List<Expense> expenses = expenseRepository.findByTripId(tripId);
    
    for (Expense expense : expenses) {
        // Person who paid gets credited
        balances.merge(expense.getPaidByMemberId(), 
            expense.getAmount(), BigDecimal::add);
        
        // Each person in the split gets debited
        List<ExpenseSplit> splits = expense.getSplits();
        for (ExpenseSplit split : splits) {
            balances.merge(split.getMemberId(), 
                split.getSplitAmount().negate(), BigDecimal::add);
        }
    }
    
    return balances;
}
```

### Ví dụ
```
A paid: 300k, owes: 100k → Balance: +200k (receive)
B paid: 0k,   owes: 100k → Balance: -100k (pay)
C paid: 0k,   owes: 100k → Balance: -100k (pay)

Optimal settlements:
- B pays A: 100k
- C pays A: 100k

Total: 2 transactions (instead of potentially more)
```

---

## 🎨 Frontend Architecture

### Component Structure

```
src/
├── components/
│   ├── ui/                      # Shadcn UI components
│   │   ├── button.tsx
│   │   ├── input.tsx
│   │   ├── dialog.tsx
│   │   ├── card.tsx
│   │   └── ...
│   ├── layout/
│   │   ├── Navbar.tsx
│   │   ├── Sidebar.tsx
│   │   └── Footer.tsx
│   ├── auth/
│   │   ├── LoginForm.tsx
│   │   ├── RegisterForm.tsx
│   │   └── SocialLogin.tsx
│   ├── trips/
│   │   ├── TripCard.tsx
│   │   ├── TripList.tsx
│   │   ├── CreateTripDialog.tsx
│   │   ├── TripDetails.tsx
│   │   ├── InviteMemberDialog.tsx
│   │   └── BudgetDisplay.tsx
│   ├── expenses/
│   │   ├── ExpenseList.tsx
│   │   ├── ExpenseCard.tsx
│   │   ├── AddExpenseDialog.tsx
│   │   └── ExpenseForm.tsx
│   ├── settlements/
│   │   ├── SettlementList.tsx
│   │   ├── SettlementCard.tsx
│   │   ├── UploadProofDialog.tsx
│   │   └── BalanceSummary.tsx
│   ├── qr/
│   │   ├── QRCodeDisplay.tsx
│   │   ├── QRCodeUpload.tsx
│   │   └── QRCodeSelector.tsx
│   ├── public-trips/
│   │   ├── PublicTripList.tsx
│   │   ├── PublicTripCard.tsx
│   │   └── TripVoting.tsx
│   └── admin/
│       ├── StatsDashboard.tsx
│       └── StatsCard.tsx
├── pages/
│   ├── HomePage.tsx
│   ├── LoginPage.tsx
│   ├── RegisterPage.tsx
│   ├── DashboardPage.tsx
│   ├── TripDetailsPage.tsx
│   ├── JoinTripPage.tsx
│   ├── PublicTripsPage.tsx
│   └── AdminPage.tsx
├── hooks/
│   ├── useAuth.ts
│   ├── useTrips.ts
│   ├── useExpenses.ts
│   └── useSettlements.ts
├── services/
│   ├── api.ts                   # Axios instance
│   ├── authService.ts
│   ├── tripService.ts
│   ├── expenseService.ts
│   └── uploadService.ts
├── store/
│   ├── authStore.ts             # Zustand
│   └── tripStore.ts
├── utils/
│   ├── formatters.ts            # Currency, date formatting
│   ├── validators.ts            # Zod schemas
│   └── constants.ts
└── App.tsx
```

### Key Pages & Flows

#### 1. Guest Flow - Create & Manage Trip
```
Home → Create Trip → Trip Details → Add Members (share link) → Add Expenses → View Settlements → Upload Proof → Confirm Payment
```

#### 2. Authenticated User Flow
```
Login → Dashboard (My Trips) → Create/View Trips → Make Trip Public → Browse Public Trips → Like Trips
```

#### 3. Join Trip Flow (Guest)
```
Click Invite Link → Preview Trip → Accept Join → Enter Nickname → View Trip
```

---

## 🚦 User Stories & Acceptance Criteria

### Priority 1 (***) - MVP Features

#### US-1: Tạo Cuộc Đi Chơi (Guest)
**As a** guest user  
**I want to** create a new trip  
**So that** I can track expenses with my friends  

**Acceptance Criteria:**
- Có thể tạo trip không cần đăng nhập
- Nhập: tên trip, mô tả, ngày bắt đầu/kết thúc, ngân sách (optional)
- System tự động generate invite code
- Người tạo được assign role CREATOR
- Redirect đến trip details sau khi tạo

#### US-2: Thêm Thành Viên (Share Link)
**As a** trip creator  
**I want to** invite members via a shareable link  
**So that** friends can join the trip  

**Acceptance Criteria:**
- Copy invite link với format: `app.com/trips/join/{inviteCode}`
- Người nhận click link → nhập nickname → accept join
- Creator có notification khi có người join
- Hiển thị danh sách members với status (PENDING, ACCEPTED)

#### US-3: Thêm Khoản Chi
**As a** trip member  
**I want to** add an expense  
**So that** everyone knows what I paid for the group  

**Acceptance Criteria:**
- Mọi member đều có thể add expense
- Nhập: số tiền, mô tả, người trả, ngày chi
- Tự động split đều cho tất cả members
- Upload receipt (optional)
- Real-time update expense list

#### US-4: Tính Toán Chia Tiền
**As a** trip member  
**I want to** see who owes whom  
**So that** we can settle payments efficiently  

**Acceptance Criteria:**
- View balance summary: mỗi người nợ/nhận bao nhiêu
- Hiển thị optimal settlements (tối ưu số giao dịch)
- Mỗi settlement hiển thị: from → to, amount, status
- Status: PENDING, PAID, CONFIRMED

#### US-5: Upload QR Code
**As a** user  
**I want to** upload my banking QR code  
**So that** members can pay me easily  

**Acceptance Criteria:**
- Upload image → lưu vào Cloudinary/MinIO
- Nhập: bank name, account number, account holder
- Có thể save multiple QR codes
- Set default QR
- Assign QR to trip (mỗi trip có 1 QR của creator)

#### US-6: Upload Minh Chứng Chuyển Tiền
**As a** payer  
**I want to** upload payment proof  
**So that** receiver can confirm  

**Acceptance Criteria:**
- Upload screenshot for each settlement
- Settlement status → PAID
- Receiver gets notification

#### US-7: Đánh Dấu Đã Nhận
**As a** receiver  
**I want to** confirm payment received  
**So that** the settlement is complete  

**Acceptance Criteria:**
- Chỉ receiver mới có button "Confirm"
- Click confirm → status → CONFIRMED
- Payer gets notification
- Không thể undo sau khi confirm

#### US-8: Lưu Danh Sách Chuyến Đi (Signed-in)
**As a** registered user  
**I want to** see all my trips  
**So that** I can manage them  

**Acceptance Criteria:**
- Dashboard hiển thị list trips (created, joined)
- Filter: ACTIVE, SETTLED, ARCHIVED
- Sort: newest, oldest, highest balance
- Quick stats: total trips, total spent, total owed

#### US-9: Admin Dashboard
**As an** admin  
**I want to** view system statistics  
**So that** I can monitor usage  

**Acceptance Criteria:**
- Total trips (all time, this month)
- Total users (all time, this month)
- Total expenses tracked
- Most active trips
- Recent activities

---

## 🔐 Security & Authorization

### Role-Based Access Control

```java
public enum TripRole {
    CREATOR,  // Full control
    MEMBER    // Can add expenses, view settlements
}

// Authorization Rules
- Create Trip: Anyone (guest or authenticated)
- Update Trip Metadata: CREATOR only
- Update Budget: CREATOR only
- Add Member: CREATOR only
- Remove Member: CREATOR only
- Add Expense: All members
- Update/Delete Expense: Expense creator or trip CREATOR
- Upload Payment Proof: Payer only
- Confirm Payment: Receiver only
- Make Trip Public: CREATOR only (requires authentication)
- Delete Trip: CREATOR only
```

### Guest vs Authenticated Users

```
Guest Users:
✅ Create trips
✅ Join trips
✅ Add expenses
✅ View settlements
✅ Upload QR/proofs
❌ View trip history (chỉ có link hiện tại)
❌ Make trip public
❌ Like other trips

Authenticated Users:
✅ All guest features
✅ View all joined trips
✅ Make trip public
✅ Like public trips
✅ Receive notifications
```

---

## 📧 Email Notifications

### Email Templates

1. **Welcome Email** (on registration)
2. **Email Verification** (verify link)
3. **Password Reset** (reset link)
4. **Trip Invitation** (join link)
5. **New Expense Added** (to all members)
6. **Payment Reminder** (to debtors)
7. **Payment Proof Uploaded** (to receiver)
8. **Payment Confirmed** (to payer)

### Notification Schedule

```java
@Scheduled(cron = "0 0 9 * * *") // Every day at 9 AM
public void sendPaymentReminders() {
    // Find all PENDING settlements > 3 days old
    // Send email reminder to payers
}
```

---

## 🎯 Development Phases

### Phase 1: Core MVP (2-3 weeks)
**Goal:** Guest users can create trips, add expenses, and settle payments

**Tasks:**
1. ✅ Setup project structure
   - Backend: Spring Boot + PostgreSQL
   - Frontend: React + Vite + Shadcn UI
2. ✅ Authentication (reuse from justauth)
   - JWT + Social OAuth
3. ✅ Trip Management
   - Create, join, view trip
4. ✅ Expense Tracking
   - Add, list, update expenses
5. ✅ Settlement Calculation
   - Implement debt simplification algorithm
6. ✅ File Upload (Cloudinary)
   - QR codes, payment proofs
7. ✅ Basic UI
   - Responsive design, mobile-first

**Deliverables:**
- Working app on localhost
- Guest users can complete full flow
- Basic error handling

### Phase 2: Authenticated Features (1-2 weeks)
**Goal:** Registered users can manage trip history

**Tasks:**
1. ✅ User Dashboard
   - List all trips (created, joined)
   - Trip statistics
2. ✅ Public Trips
   - Make trip public
   - Browse public trips
   - Like/vote system
3. ✅ Notifications
   - Email notifications
   - In-app notifications
4. ✅ Enhanced UI/UX
   - Animations, loading states
   - Better error messages

**Deliverables:**
- Complete authenticated flow
- Email system working
- Public trip gallery

### Phase 3: Admin & Polish (1 week)
**Goal:** Admin dashboard and production-ready

**Tasks:**
1. ✅ Admin Dashboard
   - Statistics overview
   - User/trip management
2. ✅ Testing
   - Unit tests (backend)
   - Integration tests
   - E2E tests (Playwright/Cypress)
3. ✅ Performance Optimization
   - Database indexing
   - Query optimization
   - Frontend lazy loading
4. ✅ Deployment
   - Docker setup
   - Vercel (frontend)
   - GCP/VPS (backend)
   - PostgreSQL deployment

**Deliverables:**
- Production deployment
- Monitoring setup
- Documentation

### Phase 4: Future Enhancements (Backlog)
- Category-based budgets
- Multi-currency support
- Export trip as PDF
- Real-time chat
- Mobile app (React Native)
- Integration with banking APIs (auto-detect payments)

---

## 🚀 Local Development Setup

### Prerequisites
```bash
- Java 17+
- Node.js 18+
- PostgreSQL 15+
- Docker & Docker Compose (optional)
```

### Backend Setup

1. **Clone justauth repo** (reuse code)
```bash
cd /path/to/justauth
# Copy security, JWT, exception handling code
```

2. **Create new Spring Boot project**
```bash
cd /Users/nguyenphucthinh/Documents/study/ai-coding/
npx spring-boot-cli init \
  --dependencies=web,security,data-jpa,postgresql,validation,mail,oauth2-client \
  --name=split-bill \
  split-bill
```

3. **Copy from justauth**
```bash
# Security config, JWT service, exception handlers
cp -r justauth/src/main/java/com/peter/justauth/security split-bill/src/main/java/com/splitbill/
cp -r justauth/src/main/java/com/peter/justauth/exception split-bill/src/main/java/com/splitbill/
```

4. **Configure application.yml**
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/splitbill_db
    username: postgres
    password: postgres
  
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
  
  mail:
    host: smtp.gmail.com
    port: 587
    username: ${MAIL_USERNAME}
    password: ${MAIL_PASSWORD}

cloudinary:
  cloud-name: ${CLOUDINARY_CLOUD_NAME}
  api-key: ${CLOUDINARY_API_KEY}
  api-secret: ${CLOUDINARY_API_SECRET}

jwt:
  secret: ${JWT_SECRET}
  expiration: 86400000 # 24h
```

5. **Run backend**
```bash
cd split-bill
./mvnw spring-boot:run
```

### Frontend Setup

1. **Create React app**
```bash
cd /Users/nguyenphucthinh/Documents/study/ai-coding/
npm create vite@latest split-bill-ui -- --template react-ts
cd split-bill-ui
```

2. **Install dependencies**
```bash
npm install
npm install -D tailwindcss postcss autoprefixer
npx tailwindcss init -p

# Shadcn UI
npx shadcn-ui@latest init
npx shadcn-ui@latest add button input card dialog form

# Other deps
npm install axios react-router-dom zustand @tanstack/react-query
npm install react-hook-form zod @hookform/resolvers
npm install date-fns lucide-react
```

3. **Configure Vite proxy** (vite.config.ts)
```typescript
export default defineConfig({
  server: {
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
})
```

4. **Run frontend**
```bash
npm run dev
# Opens at http://localhost:5173
```

### Database Setup

**Option 1: Docker Compose**
```yaml
# docker-compose.yml
version: '3.8'
services:
  postgres:
    image: postgres:15
    environment:
      POSTGRES_DB: splitbill_db
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: postgres
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data

volumes:
  postgres_data:
```

```bash
docker-compose up -d
```

**Option 2: Local PostgreSQL**
```bash
createdb splitbill_db
```

---

## 📱 UI/UX Design Principles

### Design System
- **Colors**: Vibrant, modern palette (HSL-based)
  - Primary: Blue gradient (#3B82F6 → #8B5CF6)
  - Success: Green (#10B981)
  - Warning: Orange (#F59E0B)
  - Danger: Red (#EF4444)
- **Typography**: Inter (Google Fonts)
- **Spacing**: 4px base unit
- **Radius**: 8px default, 16px for cards
- **Shadows**: Subtle elevations

### Key Screens

1. **Landing Page**
   - Hero section with CTA "Tạo Chuyến Đi Ngay"
   - Feature highlights (3 columns)
   - Sample trip showcase

2. **Trip Details**
   - Header: Trip name, dates, budget progress bar
   - Tabs: Expenses | Settlements | Members | Settings
   - FAB: Add Expense

3. **Settlement View**
   - Card for each settlement
   - Visual flow: A → B (100k VND)
   - Status badges (PENDING, PAID, CONFIRMED)
   - Upload proof button

4. **Mobile-First**
   - Bottom navigation
   - Swipe gestures
   - Touch-friendly buttons (min 44px)

---

## 🧪 Testing Strategy

### Backend Tests
```java
// Unit Tests
- Service layer logic (settlement algorithm)
- Security filters (JWT validation)

// Integration Tests
- API endpoints with TestRestTemplate
- Database operations with @DataJpaTest

// Test Coverage Goal: 80%+
```

### Frontend Tests
```typescript
// Unit Tests (Vitest)
- Utility functions (formatters, validators)
- Custom hooks

// Component Tests (React Testing Library)
- User interactions
- Form validations

// E2E Tests (Playwright)
- Complete user flows
- Cross-browser testing
```

---

## 📊 Monitoring & Analytics

### Metrics to Track
- **Business Metrics**
  - Total trips created
  - Average trip size (members, expenses)
  - Settlement completion rate
  - Public trip engagement (views, likes)

- **Technical Metrics**
  - API response times (p50, p95, p99)
  - Error rates (4xx, 5xx)
  - Database query performance
  - File upload success rate

### Tools
- **Backend**: Spring Boot Actuator + Prometheus
- **Frontend**: Vercel Analytics
- **Logs**: Logback (backend), Console (frontend)
- **Errors**: Sentry (optional)

---

## 🔄 CI/CD Pipeline

### GitHub Actions Workflow

```yaml
# .github/workflows/main.yml
name: CI/CD

on:
  push:
    branches: [ main, develop ]
  pull_request:
    branches: [ main ]

jobs:
  backend-test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - uses: actions/setup-java@v3
        with:
          java-version: '17'
      - run: ./mvnw test

  frontend-test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - uses: actions/setup-node@v3
        with:
          node-version: '18'
      - run: npm ci
      - run: npm test

  deploy-frontend:
    needs: [frontend-test]
    if: github.ref == 'refs/heads/main'
    runs-on: ubuntu-latest
    steps:
      - uses: amondnet/vercel-action@v20
        with:
          vercel-token: ${{ secrets.VERCEL_TOKEN }}

  deploy-backend:
    needs: [backend-test]
    if: github.ref == 'refs/heads/main'
    runs-on: ubuntu-latest
    steps:
      - # Build Docker image
      - # Push to GCP Container Registry
      - # Deploy to Cloud Run
```

---

## 📝 API Response Formats

### Success Response
```json
{
  "status": "success",
  "data": {
    "id": 1,
    "name": "Đà Lạt Trip",
    "members": [...]
  },
  "message": "Trip created successfully"
}
```

### Error Response
```json
{
  "status": "error",
  "error": {
    "code": "TRIP_NOT_FOUND",
    "message": "Không tìm thấy chuyến đi",
    "field": null,
    "path": "/api/trips/999"
  },
  "timestamp": "2026-01-22T23:00:00Z"
}
```

### Validation Error
```json
{
  "status": "error",
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "Dữ liệu không hợp lệ",
    "fields": {
      "amount": "Số tiền phải lớn hơn 0",
      "description": "Mô tả không được để trống"
    }
  }
}
```

---

## 🌐 Deployment Architecture

```
┌─────────────────┐
│  User Browser   │
└────────┬────────┘
         │ HTTPS
         ▼
┌─────────────────┐
│  Vercel CDN     │  ← React App
└────────┬────────┘
         │ API calls
         ▼
┌─────────────────┐
│  Load Balancer  │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│  GCP Cloud Run  │  ← Spring Boot (auto-scale)
└────────┬────────┘
         │
    ┌────┴────┬──────────────┐
    ▼         ▼              ▼
┌────────┐ ┌──────────┐ ┌────────────┐
│Postgres│ │Cloudinary│ │ Gmail SMTP │
│ Cloud  │ │   CDN    │ │   (Email)  │
└────────┘ └──────────┘ └────────────┘
```

### Environment Variables

**.env.backend**
```
DATABASE_URL=jdbc:postgresql://...
JWT_SECRET=your-secret-key
CLOUDINARY_CLOUD_NAME=...
CLOUDINARY_API_KEY=...
CLOUDINARY_API_SECRET=...
MAIL_USERNAME=...
MAIL_PASSWORD=...
GOOGLE_CLIENT_ID=...
GOOGLE_CLIENT_SECRET=...
FACEBOOK_CLIENT_ID=...
FACEBOOK_CLIENT_SECRET=...
```

**.env.frontend**
```
VITE_API_URL=https://api.splitbill.com
VITE_APP_URL=https://splitbill.com
```

---

## 📖 Glossary

- **Trip**: Chuyến đi chơi
- **Expense**: Khoản chi
- **Settlement**: Giao dịch thanh toán (A → B)
- **Balance**: Số dư (mỗi người nợ/nhận bao nhiêu)
- **Split**: Phần chia (split expense equally)
- **QR Code**: Mã QR banking để nhận tiền
- **Proof**: Minh chứng chuyển khoản
- **Invite Code**: Mã mời tham gia trip
- **Public Trip**: Chuyến đi công khai (có thể xem, like)

---

## ✅ Checklist Before Production

### Security
- [ ] HTTPS enabled (SSL certificate)
- [ ] CORS configured properly
- [ ] SQL injection prevention (use JPA)
- [ ] XSS prevention (sanitize inputs)
- [ ] Rate limiting on APIs
- [ ] JWT secret rotation plan
- [ ] Environment variables secured

### Performance
- [ ] Database indexes on foreign keys
- [ ] Lazy loading for large lists
- [ ] Image optimization (compression)
- [ ] API response caching (Caffeine)
- [ ] Frontend code splitting

### UX
- [ ] Loading states on all async actions
- [ ] Error messages in Vietnamese
- [ ] Mobile responsive (test on real devices)
- [ ] Accessibility (ARIA labels, keyboard nav)
- [ ] Offline handling (show appropriate message)

### Data
- [ ] Database backup strategy
- [ ] Data migration plan
- [ ] GDPR compliance (if needed)
- [ ] User data export feature

### Monitoring
- [ ] Error tracking (Sentry)
- [ ] Performance monitoring (Actuator)
- [ ] Uptime monitoring (UptimeRobot)
- [ ] Analytics (GA4)

---

## 🤝 Contributing

(For future team members)

### Git Workflow
```
main (production)
  ↑
develop (staging)
  ↑
feature/trip-management
feature/settlement-algorithm
bugfix/payment-confirmation
```

### Commit Convention
```
feat: Add settlement debt simplification algorithm
fix: Fix QR upload failing for large images
docs: Update API documentation for settlements
test: Add unit tests for expense service
refactor: Extract calculation logic to utility class
```

---

## 📞 Support & Contact

- **Developer**: Nguyễn Phúc Thịnh
- **Email**: [your-email]
- **Project Repo**: [GitHub link]

---

**Last Updated**: 2026-01-22  
**Version**: 1.0.0  
**Status**: 🚧 In Development
