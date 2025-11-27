# Use a minimal Alpine-based Python image for a smaller attack surface.
FROM python:3.9.18-alpine3.18

# Set the working directory in the container to /app.
WORKDIR /app

# Create a non-root user and group for security.
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Copy the requirements file first to leverage Docker's layer caching.
# The dependency installation step will only be re-run if this file changes.
COPY requirements.txt .

# Install any needed packages specified in requirements.txt.
# Use --no-cache-dir to keep the image size down.
RUN pip install --no-cache-dir -r requirements.txt

# Copy the application source code from the local 'src' directory
# into the '/app' directory in the container.
COPY src/ .

# Change ownership of the files to the non-root user.
RUN chown -R appuser:appgroup /app

# Switch to the non-root user.
USER appuser

# Make port 5000 available to the world outside this container.
EXPOSE 5000

# Define the command to run the application when the container launches.
CMD ["python", "app.py"]
