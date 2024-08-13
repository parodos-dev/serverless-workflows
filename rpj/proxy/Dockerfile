# Start with a minimal Red Hat Universal Base Image (UBI)
FROM registry.access.redhat.com/ubi8/python-39

# Set the working directory
WORKDIR /app

# Copy the requirements file first
COPY requirements.txt .

# Install required packages
RUN pip install --no-cache-dir -r requirements.txt

# Copy the application code into the container
COPY . .

# Expose the port for the Gunicorn server
EXPOSE 8080

# Command to run the application using Gunicorn
CMD ["gunicorn", "-b", "0.0.0.0:8080", "app:app", "--workers", "4"]