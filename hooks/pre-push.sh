#!/bin/bash

echo "🔍 Running code lint check..."
mvn spotless:check
if [ $? -ne 0 ]; then
  echo "❌ Code convention issues detected. Push aborted."
  exit 1
fi

echo "✅ Lint passed. Running unit tests..."
mvn test -q
if [ $? -ne 0 ]; then
  echo "❌ Unit tests failed. Push aborted."
  exit 1
fi

echo "✅ All checks passed. Continue pushing."
exit 0
