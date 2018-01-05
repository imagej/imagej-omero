FROM maven:3-jdk-8-alpine

# Install Git
RUN apk update && \
  apk upgrade && \
  apk add git

# Imagej-OMERO and Imglib2-roi
RUN cd / && \
  mkdir imagej-omero && \
  git clone https://github.com/imglib/imglib2-roi.git
COPY . imagej-omero

# Checkout and build shape-rois
RUN cd /imglib2-roi && \
  git checkout shape-rois && \
  mvn clean install -DskipTests

# Test
WORKDIR /imagej-omero
RUN mvn clean install -DskipTests
CMD mvn failsafe:integration-test failsafe:verify -DskipITs=false
