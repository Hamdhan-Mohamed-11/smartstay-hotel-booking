# Publishing SmartStay to GitHub

This folder is already a git repository with commit history. To publish it:

1. Sign in to GitHub and create a new **empty** repository named `smartstay-hotel-booking`
   (no README, .gitignore or licence).
2. In the VS Code terminal inside this folder run:

```sh
git remote add origin https://github.com/YOUR_USERNAME/smartstay-hotel-booking.git
git push -u origin main
```

3. If the repository is private, add your lecturer as a collaborator (Settings → Collaborators),
   or make it public if your institution allows it. Open the link in a private browser window to
   confirm it can be viewed.
4. Paste the repository URL into section 5 of the report and re-export the PDF.

After later changes:

```sh
git add .
git commit -m "Describe the change"
git push
```

`data/`, `out/` and `*.class` are ignored, so guest records and build output are never uploaded.
