#!/usr/bin/env python3
"""
Release management script for Kotlin Hybrid Architecture Template.

This script handles the complete release process including:
- Version updates across all markdown files
- Gradle version updates
- CHANGELOG.md maintenance
- Git tagging and GitHub release creation

Usage:
    python scripts/release.py prepare <version>
    python scripts/release.py release <version>
    python scripts/release.py publish <version>

Examples:
    python scripts/release.py prepare 1.0.0
    python scripts/release.py release 1.0.0
    python scripts/release.py publish 1.0.0
"""

import argparse
import glob
import os
import re
import subprocess
import sys
from datetime import datetime
from pathlib import Path
from typing import List, Optional


class ReleaseManager:
    def __init__(self, project_root: str):
        self.project_root = Path(project_root)
        self.date_str = datetime.now().strftime("%B %Y")
        
    def find_markdown_files(self) -> List[Path]:
        """Find all markdown files with version headers."""
        md_files = []
        for pattern in ["**/*.md", "**/README.md"]:
            md_files.extend(self.project_root.glob(pattern))
        
        # Filter to only files that have version headers
        versioned_files = []
        for md_file in md_files:
            try:
                with open(md_file, 'r', encoding='utf-8') as f:
                    content = f.read()
                    if re.search(r'\*\*Version:\*\*', content):
                        versioned_files.append(md_file)
            except Exception as e:
                print(f"Warning: Could not read {md_file}: {e}")
                
        return versioned_files
    
    def update_markdown_version(self, file_path: Path, new_version: str) -> bool:
        """Update version and date in markdown file header."""
        try:
            with open(file_path, 'r', encoding='utf-8') as f:
                content = f.read()
            
            # Update version
            content = re.sub(
                r'\*\*Version:\*\* [^\s]+',
                f'**Version:** {new_version}',
                content
            )
            
            # Update date
            content = re.sub(
                r'\*\*Date:\*\* [^\n]+',
                f'**Date:** {self.date_str}',
                content
            )
            
            with open(file_path, 'w', encoding='utf-8') as f:
                f.write(content)
                
            return True
            
        except Exception as e:
            print(f"Error updating {file_path}: {e}")
            return False
    
    def update_gradle_version(self, new_version: str) -> bool:
        """Update version in root build.gradle.kts."""
        gradle_file = self.project_root / "build.gradle.kts"
        
        try:
            with open(gradle_file, 'r', encoding='utf-8') as f:
                content = f.read()
            
            # Update version line
            content = re.sub(
                r'version = "[^"]+"',
                f'version = "{new_version}"',
                content
            )
            
            with open(gradle_file, 'w', encoding='utf-8') as f:
                f.write(content)
                
            return True
            
        except Exception as e:
            print(f"Error updating gradle version: {e}")
            return False
    
    def update_changelog(self, new_version: str) -> bool:
        """Update CHANGELOG.md with new version."""
        changelog_file = self.project_root / "CHANGELOG.md"
        
        try:
            with open(changelog_file, 'r', encoding='utf-8') as f:
                content = f.read()
            
            # Find the [Unreleased] section and replace it
            unreleased_pattern = r'## \[Unreleased\]\s*\n(.*?)(?=\n## |\Z)'
            match = re.search(unreleased_pattern, content, re.DOTALL)
            
            if match:
                unreleased_content = match.group(1).strip()
                
                # Create new release section
                release_section = f"""## [{new_version}] - {self.date_str}

{unreleased_content}

## [Unreleased]

### Added


### Changed


### Fixed


### Removed


### Security

"""
                
                # Replace the unreleased section
                content = re.sub(
                    r'## \[Unreleased\]\s*\n.*?(?=\n## |\Z)',
                    release_section,
                    content,
                    flags=re.DOTALL
                )
                
                with open(changelog_file, 'w', encoding='utf-8') as f:
                    f.write(content)
                    
                return True
            else:
                print("Could not find [Unreleased] section in CHANGELOG.md")
                return False
                
        except Exception as e:
            print(f"Error updating changelog: {e}")
            return False
    
    def run_command(self, cmd: List[str], capture_output: bool = False) -> Optional[str]:
        """Run a shell command."""
        try:
            result = subprocess.run(
                cmd, 
                cwd=self.project_root,
                capture_output=capture_output,
                text=True,
                check=True
            )
            return result.stdout if capture_output else None
        except subprocess.CalledProcessError as e:
            print(f"Command failed: {' '.join(cmd)}")
            print(f"Error: {e}")
            if e.stdout:
                print(f"Stdout: {e.stdout}")
            if e.stderr:
                print(f"Stderr: {e.stderr}")
            return None
    
    def verify_clean_working_tree(self) -> bool:
        """Verify git working tree is clean."""
        result = self.run_command(["git", "status", "--porcelain"], capture_output=True)
        if result is None:
            return False
        return len(result.strip()) == 0
    
    def verify_tests_pass(self) -> bool:
        """Run all tests and quality checks."""
        print("Running tests and quality checks...")
        
        commands = [
            ["./gradlew", "clean", "build", "test", "check"],
            ["make", "test-arch"]
        ]
        
        for cmd in commands:
            print(f"Running: {' '.join(cmd)}")
            if self.run_command(cmd) is None:
                return False
                
        return True
    
    def verify_security_scan(self) -> bool:
        """Run security dependency check."""
        print("Running security scan...")
        return self.run_command(["./gradlew", "dependencyCheckAggregate"]) is not None
    
    def create_git_tag(self, version: str) -> bool:
        """Create annotated git tag."""
        tag_name = f"v{version}"
        message = f"Release version {version}"
        
        return self.run_command([
            "git", "tag", "-a", tag_name, "-m", message
        ]) is not None
    
    def push_changes(self, version: str) -> bool:
        """Push changes and tags to origin."""
        commands = [
            ["git", "push", "origin", "main"],
            ["git", "push", "origin", f"v{version}"]
        ]
        
        for cmd in commands:
            if self.run_command(cmd) is None:
                return False
                
        return True
    
    def build_distribution_artifacts(self, version: str) -> bool:
        """Build distribution artifacts including JARs and docs."""
        print("Building distribution artifacts...")
        
        commands = [
            ["./gradlew", "clean", "build"],
            ["./gradlew", "jar", "sourcesJar", "javadocJar"],
            ["make", "diagrams"]  # Generate PlantUML diagrams
        ]
        
        for cmd in commands:
            print(f"Running: {' '.join(cmd)}")
            if self.run_command(cmd) is None:
                return False
                
        return True
    
    def create_github_release(self, version: str) -> bool:
        """Create GitHub release using gh CLI with artifacts."""
        # Extract release notes from CHANGELOG.md
        changelog_file = self.project_root / "CHANGELOG.md"
        release_notes = ""
        
        try:
            with open(changelog_file, 'r', encoding='utf-8') as f:
                content = f.read()
            
            # Find the current version section
            version_pattern = rf'## \[{re.escape(version)}\][^\n]*\n(.*?)(?=\n## |\Z)'
            match = re.search(version_pattern, content, re.DOTALL)
            
            if match:
                release_notes = match.group(1).strip()
            else:
                release_notes = f"Release version {version}"
                
        except Exception as e:
            print(f"Warning: Could not extract release notes: {e}")
            release_notes = f"Release version {version}"
        
        # Build distribution artifacts
        if not self.build_distribution_artifacts(version):
            print("❌ Failed to build distribution artifacts")
            return False
        
        # Find JAR files to upload
        jar_files = []
        for module in ["bootstrap", "domain", "application", "infrastructure", "presentation"]:
            module_jars = list(self.project_root.glob(f"{module}/build/libs/*.jar"))
            jar_files.extend(module_jars)
        
        # Create release command
        cmd = [
            "gh", "release", "create", f"v{version}",
            "--title", f"Release {version}",
            "--notes", release_notes
        ]
        
        # Add JAR files as assets
        for jar_file in jar_files:
            cmd.extend([str(jar_file)])
        
        return self.run_command(cmd) is not None
    
    def prepare_release(self, version: str) -> bool:
        """Prepare release by updating versions and running checks."""
        print(f"Preparing release {version}...")
        
        # Update markdown files
        md_files = self.find_markdown_files()
        print(f"Updating {len(md_files)} markdown files...")
        
        for md_file in md_files:
            if not self.update_markdown_version(md_file, version):
                return False
            print(f"  ✓ {md_file.relative_to(self.project_root)}")
        
        # Update gradle version
        print("Updating Gradle version...")
        if not self.update_gradle_version(version):
            return False
        print("  ✓ build.gradle.kts")
        
        # Update changelog
        print("Updating CHANGELOG.md...")
        if not self.update_changelog(version):
            return False
        print("  ✓ CHANGELOG.md")
        
        # Run tests and checks
        if not self.verify_tests_pass():
            print("❌ Tests or quality checks failed")
            return False
        print("  ✓ All tests and quality checks passed")
        
        # Run security scan
        if not self.verify_security_scan():
            print("❌ Security scan failed")
            return False
        print("  ✓ Security scan passed")
        
        print(f"\n✅ Release {version} prepared successfully!")
        print("\nNext steps:")
        print("1. Review the changes")
        print("2. Commit the changes: git add -A && git commit -m 'Prepare release {version}'")
        print(f"3. Run: python scripts/release.py release {version}")
        
        return True
    
    def create_release(self, version: str) -> bool:
        """Create the actual release."""
        print(f"Creating release {version}...")
        
        # Verify working tree is clean
        if not self.verify_clean_working_tree():
            print("❌ Working tree is not clean. Please commit changes first.")
            return False
        
        # Create git tag
        print("Creating git tag...")
        if not self.create_git_tag(version):
            return False
        print(f"  ✓ Created tag v{version}")
        
        # Push changes and tag
        print("Pushing to origin...")
        if not self.push_changes(version):
            return False
        print("  ✓ Pushed changes and tag")
        
        # Create GitHub release
        print("Creating GitHub release...")
        if not self.create_github_release(version):
            return False
        print("  ✓ Created GitHub release")
        
        print(f"\n🎉 Release {version} created successfully!")
        return True
    
    def verify_maven_credentials(self) -> bool:
        """Verify Maven publishing credentials are available."""
        username = os.getenv("GPR_USER") or os.getenv("GITHUB_ACTOR")
        token = os.getenv("GPR_TOKEN") or os.getenv("GITHUB_TOKEN")
        
        if not username or not token:
            print("❌ Maven publishing credentials not found")
            print("Required environment variables:")
            print("  - GPR_USER (or GITHUB_ACTOR)")
            print("  - GPR_TOKEN (or GITHUB_TOKEN)")
            print("\nTo set up GitHub Packages publishing:")
            print("1. Create a personal access token with 'write:packages' scope")
            print("2. Export GPR_USER=your-github-username")
            print("3. Export GPR_TOKEN=your-personal-access-token")
            return False
            
        return True
    
    def publish_to_maven(self, version: str) -> bool:
        """Publish artifacts to GitHub Packages Maven repository."""
        print(f"Publishing {version} to GitHub Packages...")
        
        # Verify credentials
        if not self.verify_maven_credentials():
            return False
        
        # Build and publish
        commands = [
            ["./gradlew", "clean", "build"],
            ["./gradlew", "publishAllPublicationsToGitHubPackagesRepository"]
        ]
        
        for cmd in commands:
            print(f"Running: {' '.join(cmd)}")
            if self.run_command(cmd) is None:
                return False
        
        print(f"✅ Successfully published {version} to GitHub Packages")
        print(f"📦 Packages available at: https://github.com/your-username/kotlin-hybrid-architecture-template/packages")
        return True


def main():
    parser = argparse.ArgumentParser(
        description="Release management for Kotlin Hybrid Architecture Template"
    )
    parser.add_argument(
        "action", 
        choices=["prepare", "release", "publish"],
        help="Action to perform"
    )
    parser.add_argument(
        "version",
        help="Version to release (e.g., 1.0.0)"
    )
    
    args = parser.parse_args()
    
    # Validate version format
    if not re.match(r'^\d+\.\d+\.\d+$', args.version):
        print("Error: Version must be in format X.Y.Z (e.g., 1.0.0)")
        sys.exit(1)
    
    # Find project root
    script_dir = Path(__file__).parent
    project_root = script_dir.parent
    
    release_manager = ReleaseManager(str(project_root))
    
    if args.action == "prepare":
        success = release_manager.prepare_release(args.version)
    elif args.action == "release":
        success = release_manager.create_release(args.version)
    elif args.action == "publish":
        success = release_manager.publish_to_maven(args.version)
    else:
        print(f"Unknown action: {args.action}")
        sys.exit(1)
    
    sys.exit(0 if success else 1)


if __name__ == "__main__":
    main()