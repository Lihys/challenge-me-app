# "Challenge Me" - An Android App

A social habit-tracking Android app.
Friends create or join shared challenges (gym attendance, etc...), post daily check-ins with proof to show their friends, earn points, and compete on a leaderboard. 
The app is here to motivate via social accountability, fun and gamification.

---

## Tools Used

- **Language:** Kotlin
- **UI:** Jetpack Compose
- **Back end:** Firebase Auth, Firestore, Firebase Storage
- **Navigation:** Navigation Compose
- **Image loading:** Coil
- **Animations:** Lottie
- **Location:** FusedLocationProvider + Geocoder (for GPS proof)

---

## App Architecture
The files are separated to these categories:
- repository layer : screens ever call Firebase directly, but refer to ChallengeRepo, ProofRepo, UserRepo and AuthRepository
- ui/screens
- ui/components - for ui components
- ui/theme - colors
- auth/ - for authentication logic
- data/ - repositories and models
- navigations/ - NavHost and route definitions

---

## Screens and Features

- **LaunchScreen** - App entry with branding, used a Lottie animation
- **LogInScreen and RegisterScreen**  - log in and register via Firebase Auth, with an option to sign in with Google
- **HomePageScreen** - where the user sees all the challenges they're in, and have quick access to creating or joining challenges.
- **NewChallengeScreen** - where you create a challenge (name, description, rules)
- **InviteCodeScreen** - Generates a shareable invite code for a challenge, that you can share with friends.
- **JoinViaCode** - Lets a user join an existing challenge using a code.
- **ChallengePage** - The challenge feed, where you see your friends' check-ins, proofs, and points. With a leaderboard podium graphic.
- **LeaderboardPage** - The full leaderboard. you can toggle between weekly/total.
- **MyAccount** - where you can set and edit your profile pic and name. You also see some stats (how many challenge wins you got, total points) 
- **CelebrationScreen** - When a challenge ended, we see this. a celebration Lottie animation + winner announcement 

### Feature highlights

- **Daily check ins with proof** — you can prove you completed the challenge by sharing a photo and/or a GPS location
- **Points system** — you get points for checking in, and more points for photo proof, or GPS proof. ALSO a **team bonus** when two or more members checked in, on the same day.
- **Leaderboard** — toggle between "This Week" and "Total" points, computed from Firestore data including team bonuses
- **GPS location appears with a readable names** — GPS coordinates converted to a readable location name on-device, which you can also tap to open the Maps app on your phone.
- **Win counter** — when a challenge ends, a winner is decided and recorded. ,this also shows on the user's profile stats

---

## Libraries and what they're used for

- **Firebase Auth** - User sign-up/login
- **Firebase Firestore** - Challenges, check-ins, users, leaderboard, team bonus data
- **Firebase Storage** - Storing proof photos and profile pictures
- **Coil** - Async image loading (profile photos, proof photos)
- **Lottie** - Animations on the launch screen and celebration screen
- **Navigation Compose** - Screen navigation
- **FusedLocationProvider** - Getting the device's current GPS location 
- **Geocoder** - Converting GPS coordinates into actually readable location name 

