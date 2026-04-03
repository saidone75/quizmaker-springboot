#!/bin/bash

#
# QuizMaker - fun quizzes for curious minds
# Copyright (C) 2026 Saidone
#
# This program is free software: you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation, either version 3 of the License, or
# (at your option) any later version.
#
#  This program is distributed in the hope that it will be useful,
#  but WITHOUT ANY WARRANTY; without even the implied warranty of
#  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
#  GNU General Public License for more details.
#
#  You should have received a copy of the GNU General Public License
#  along with this program.  If not, see <http://www.gnu.org/licenses/>.
#

JAVA_OPTS="-Xms64m -Xmx64m -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=0.0.0.0:9000"

export SPRING_PROFILES_ACTIVE=dev

mvn spring-boot:run -Dspring-boot.run.jvmArguments="$JAVA_OPTS"
