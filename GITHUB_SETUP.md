# Publish SmartStay to your GitHub account

A GitHub email address is not a repository owner name. Sign in to the GitHub account associated with hamdhanm30@gmail.com and use the **username displayed by that account** in repository URLs.

## First run the project

Extract the ZIP, open SmartStay in VS Code, run the application and test scripts. This is a desktop terminal application; it does not require web hosting and will not run on GitHub Pages.

## Create the repository

1. Sign in to https://github.com using your own account.
2. Choose New repository. Name it `smartstay-hotel-booking`.
3. Choose Private initially. Do not add a README, .gitignore or licence because the local project already contains its files.
4. Create the repository and copy its HTTPS URL.
5. Open the VS Code terminal inside SmartStay and run:

```sh
git init
git add .
git commit -m "Implement SmartStay hotel booking coursework"
git branch -M main
git remote add origin https://github.com/YOUR_USERNAME/smartstay-hotel-booking.git
git push -u origin main
```

Replace YOUR_USERNAME with your actual username. Authenticate using Git's browser/credential-manager prompt. Your email alone does not authenticate a push. If Git asks for an author identity, configure it for this repository:

```sh
git config user.name "Hamdhan Mohamed Ameen"
git config user.email "hamdhanm30@gmail.com"
```

Then repeat the commit and push. For a public repository you may instead use the GitHub-provided no-reply commit email from your account settings.

## Share for marking

A private repository URL alone does not grant the lecturer access. Add the lecturer's verified GitHub account as a collaborator, or use a public repository only if your institution permits publishing coursework. Check access as the intended viewer before submission.

Copy the actual repository URL into report section 5, replace its pending-upload sentence, and export the updated Word document to PDF. Submit the required ZIP through ELMS as well.

The supplied .gitignore excludes data/, out/ and *.class. Keep real guest identity/contact data and credentials out of the repository. The evidence uses fictional sample guest details.

## Future changes

```sh
git add .
git commit -m "Describe your changes"
git push
```

Official guidance: GitHub (n.d.) Adding locally hosted code to GitHub. Available at: https://docs.github.com/en/migrations/importing-source-code/using-the-command-line-to-import-source-code/adding-locally-hosted-code-to-github (Accessed: 5 November 2026).
