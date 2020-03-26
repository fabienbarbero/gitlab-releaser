FROM openjdk:11-jre-slim

ADD build/distributions/gitlab-releaser-*.tar /opt/gitlab-releaser
RUN chmod +x /opt/gitlab-releaser/bin/gitlab-releaser && ln -s /opt/gitlab-releaser/bin/gitlab-releaser /usr/bin/gitlab-releaser