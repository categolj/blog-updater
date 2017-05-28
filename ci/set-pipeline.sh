#!/bin/sh
echo y | fly -t home sp -p blog-blog-updater -c pipeline.yml -l ../../credentials.yml
