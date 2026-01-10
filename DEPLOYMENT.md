# Deploy EventFinder to Koyeb (Free, No Credit Card)

This guide will help you deploy your Event Recommender app to the internet for free using Koyeb.

## Prerequisites

1. A GitHub account (to push your code)
2. Your code pushed to a GitHub repository

---

## Step 1: Push Code to GitHub

If you haven't already, push your code to GitHub:

```bash
# Initialize git if needed
git init

# Add all files
git add .

# Commit
git commit -m "Ready for Koyeb deployment"

# Add your GitHub repo as remote (replace with your repo URL)
git remote add origin https://github.com/YOUR_USERNAME/Personalized-Event-Recommendation-Engine.git

# Push to GitHub
git push -u origin main
```

---

## Step 2: Sign Up for Koyeb

1. Go to **[https://www.koyeb.com](https://www.koyeb.com)**
2. Click **"Get Started Free"**
3. Sign up with your **GitHub account** (easiest option)
4. **No credit card required!**

---

## Step 3: Create a New App on Koyeb

1. After logging in, click **"Create App"** or **"Create Service"**

2. Select **"GitHub"** as the deployment method

3. **Connect your GitHub account** if prompted

4. **Select your repository**: `Personalized-Event-Recommendation-Engine`

5. **Configure the build:**
   - **Builder**: Docker (it will auto-detect your Dockerfile)
   - **Branch**: `main` (or your default branch)

6. **Configure the service:**
   - **Instance type**: Select **"Free"** (nano instance)
   - **Port**: `8080`
   - **Region**: Choose the closest to you

7. **App name**: Choose something like `eventfinder` or `event-recommender`

8. Click **"Deploy"**

---

## Step 4: Wait for Deployment

- Koyeb will:
  1. Clone your repository
  2. Build the Docker image (this takes ~3-5 minutes first time)
  3. Deploy and start your application

- You can watch the build logs in real-time

---

## Step 5: Access Your App

Once deployed, Koyeb will give you a URL like:

```
https://eventfinder-YOUR_USERNAME.koyeb.app
```

**That's it! Your app is now live on the internet!** 🎉

---

## Troubleshooting

### Build Fails
- Check the build logs in Koyeb dashboard
- Make sure your `pom.xml` and `Dockerfile` are in the root directory

### App Crashes on Start
- Check the runtime logs in Koyeb
- The app needs ~30-60 seconds to fully start (Java/Tomcat startup time)

### MongoDB Connection Issues
- The MongoDB Atlas connection is already configured
- Make sure your MongoDB Atlas cluster allows connections from anywhere (0.0.0.0/0)

---

## Updating Your App

When you push changes to GitHub, Koyeb will automatically:
1. Detect the changes
2. Rebuild the Docker image
3. Redeploy your app

Just `git push` and wait a few minutes!

---

## Free Tier Limits

Koyeb's free tier includes:
- 1 nano instance (512MB RAM, shared CPU)
- Always-on (doesn't sleep like Render)
- Automatic HTTPS
- Custom domains supported

This is perfect for a portfolio project or demo!

---

## Share Your App

Once deployed, add the live URL to your GitHub README:

```markdown
## 🌐 Live Demo

**[Try EventFinder Live](https://your-app-name.koyeb.app)**
```

---

## Need Help?

- [Koyeb Documentation](https://www.koyeb.com/docs)
- [Koyeb Discord Community](https://discord.gg/koyeb)
