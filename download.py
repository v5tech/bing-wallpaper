
import requests
import re

# Path to the README file
readme_file_path = 'README.md'

# Function to download images
def download_image(i, url, file_name):
    response = requests.get(url)
    if response.status_code == 200:
        with open(file_name, 'wb') as file:
            file.write(response.content)
        print(f"✅ [{i + 1}]: {file_name}")
    else:
        print(f"🥺 [{i + 1}]: Failed to download {url}")
# Function to scrape image URLs from README file
def scrape_image_urls(file_path):
    with open(file_path, 'r') as file:
        content = file.read()

    # Regular expression to find all URLs that end with image file extensions
    image_urls = re.findall(r'(https?://[^\s]+\.jpg|png|jpeg|gif)', content)
    return image_urls

# Scrape image URLs from the README file
image_urls = set(scrape_image_urls(readme_file_path))

# Download each image
for i, url in enumerate(image_urls):
    file_name = url.split('OHR.')[1]
    download_image(i, url, file_name)
