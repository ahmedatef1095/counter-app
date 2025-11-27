# Use an official lightweight Python runtime as a parent image.
FROM python:3.9-slim

# Set the working directory in the container to /app.
WORKDIR /app

# Copy the requirements file first to leverage Docker's layer caching.
# The dependency installation step will only be re-run if this file changes.
COPY requirements.txt .

# Install any needed packages specified in requirements.txt.
RUN pip install --no-cache-dir -r requirements.txt

# Copy the application source code from the local 'src' directory
# into the '/app' directory in the container.
COPY src/ .

# Make port 5000 available to the world outside this container.
EXPOSE 5000

# Define the command to run the application when the container launches.
CMD ["python", "app.py"]
