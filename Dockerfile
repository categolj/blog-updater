FROM projectriff/java-function-invoker:0.0.2

ARG FUN
ARG FUN_CLASS

ADD ${FUN} /fun.jar
ENV FUNCTION_URI file:///fun.jar?handler=${FUN_CLASS}