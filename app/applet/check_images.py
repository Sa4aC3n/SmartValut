import os

print("Checking workspace for image files...")
for root, dirs, files in os.walk("."):
    for file in files:
        if file.lower().endswith(('.png', '.jpg', '.jpeg', '.webp')):
            print(os.path.join(root, file))
