#!/bin/bash

# DroneInventoryScanner Build Script
# This script helps build the application

set -e

echo "==================================="
echo "DroneInventoryScanner Build Script"
echo "==================================="
echo ""

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Check if running in project directory
if [ ! -f "settings.gradle.kts" ]; then
    echo -e "${RED}Error: Must run from project root directory${NC}"
    exit 1
fi

# Function to check prerequisites
check_prerequisites() {
    echo "Checking prerequisites..."
    
    # Check Java
    if command -v java &> /dev/null; then
        JAVA_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}')
        echo -e "${GREEN}✓${NC} Java found: $JAVA_VERSION"
    else
        echo -e "${RED}✗${NC} Java not found. Please install JDK 11 or later."
        exit 1
    fi
    
    # Check gradlew
    if [ -f "gradlew" ]; then
        echo -e "${GREEN}✓${NC} Gradle wrapper found"
        chmod +x gradlew
    else
        echo -e "${RED}✗${NC} gradlew not found"
        exit 1
    fi
    
    echo ""
}

# Function to run unit tests
run_tests() {
    echo "Running unit tests..."
    ./gradlew -b build-simple.gradle.kts test --no-daemon
    
    if [ $? -eq 0 ]; then
        echo -e "${GREEN}✓${NC} All tests passed"
    else
        echo -e "${RED}✗${NC} Tests failed"
        exit 1
    fi
    echo ""
}

# Function to build debug APK
build_debug() {
    echo "Building debug APK..."
    echo -e "${YELLOW}Note: This requires Android SDK and network access to download dependencies${NC}"
    
    if ./gradlew assembleDebug --no-daemon; then
        echo -e "${GREEN}✓${NC} Debug APK built successfully"
        echo "Location: app/build/outputs/apk/debug/app-debug.apk"
    else
        echo -e "${RED}✗${NC} Build failed"
        echo "Please check that Android SDK is installed and configured"
        exit 1
    fi
    echo ""
}

# Function to build release APK
build_release() {
    echo "Building release APK..."
    
    if [ ! -f "keystore.properties" ]; then
        echo -e "${YELLOW}Warning: keystore.properties not found${NC}"
        echo "Release builds require a signing key"
        echo "See BUILD_INSTRUCTIONS.md for details"
        exit 1
    fi
    
    if ./gradlew assembleRelease --no-daemon; then
        echo -e "${GREEN}✓${NC} Release APK built successfully"
        echo "Location: app/build/outputs/apk/release/app-release.apk"
    else
        echo -e "${RED}✗${NC} Build failed"
        exit 1
    fi
    echo ""
}

# Function to clean build
clean_build() {
    echo "Cleaning build artifacts..."
    ./gradlew clean --no-daemon
    echo -e "${GREEN}✓${NC} Build cleaned"
    echo ""
}

# Function to show help
show_help() {
    cat << EOF
Usage: ./build.sh [option]

Options:
    test        Run unit tests only
    debug       Build debug APK (requires Android SDK)
    release     Build signed release APK (requires keystore)
    clean       Clean build artifacts
    all         Run tests and build debug APK
    help        Show this help message

Examples:
    ./build.sh test       # Run tests
    ./build.sh debug      # Build debug APK
    ./build.sh all        # Test and build

EOF
}

# Main script logic
main() {
    check_prerequisites
    
    case "${1:-all}" in
        test)
            run_tests
            ;;
        debug)
            build_debug
            ;;
        release)
            build_release
            ;;
        clean)
            clean_build
            ;;
        all)
            run_tests
            build_debug
            ;;
        help)
            show_help
            ;;
        *)
            echo -e "${RED}Unknown option: $1${NC}"
            show_help
            exit 1
            ;;
    esac
    
    echo -e "${GREEN}Build script completed successfully!${NC}"
}

# Run main function
main "$@"
