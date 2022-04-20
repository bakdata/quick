{{/* vim: set filetype=mustache: */}}
{{/*
Expand the name of the chart.
*/}}
{{- define "quick.manager.name" -}}
{{- .Values.manager.name | trunc 63 | trimSuffix "-" }}
{{- end }}

{{- define "quick.ingest.name" -}}
{{- .Values.ingest.name | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Create a default fully qualified app name.
We truncate at 63 chars because some Kubernetes name fields are limited to this (by the DNS naming spec).
If release name contains chart name it will be used as a full name.
*/}}
{{- define "quick.manager.fullname" -}}
{{- if .Values.manager.name }}
{{- .Values.manager.name | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- $name := .Values.manager.name }}
{{- if contains $name .Release.Name }}
{{- .Release.Name | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- printf "%s-%s" .Release.Name $name | trunc 63 | trimSuffix "-" }}
{{- end }}
{{- end }}
{{- end }}

{{- define "quick.ingest.fullname" -}}
{{- if .Values.ingest.name }}
{{- .Values.ingest.name | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- $name := .Values.ingest.name }}
{{- if contains $name .Release.Name }}
{{- .Release.Name | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- printf "%s-%s" .Release.Name $name | trunc 63 | trimSuffix "-" }}
{{- end }}
{{- end }}
{{- end }}

{{/*
Create chart name and version as used by the chart label.
*/}}
{{- define "quick.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Common labels
*/}}
{{- define "quick.manager.labels" -}}
helm.sh/chart: {{ include "quick.chart" . }}
{{ include "quick.manager.selectorLabels" . }}
{{- if .Chart.AppVersion }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- end }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end }}

{{- define "quick.ingest.labels" -}}
helm.sh/chart: {{ include "quick.chart" . }}
{{ include "quick.ingest.selectorLabels" . }}
{{- if .Chart.AppVersion }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- end }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end }}

{{- define "quick.labels" -}}
helm.sh/chart: {{ include "quick.chart" . }}
{{- if .Chart.AppVersion }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- end }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end }}

{{/*
Selector labels
*/}}
{{- define "quick.manager.selectorLabels" -}}
app.kubernetes.io/name: {{ include "quick.manager.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}

{{- define "quick.ingest.selectorLabels" -}}
app.kubernetes.io/name: {{ include "quick.ingest.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}

{{- define "quick.manager.service.account.name" -}}
{{- printf "%s-service-account" .Values.manager.name }}
{{- end }}
