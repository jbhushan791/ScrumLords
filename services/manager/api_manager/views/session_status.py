import base64
import json

from rest_framework import status
from rest_framework.decorators import api_view
from rest_framework.generics import get_object_or_404
from rest_framework.response import Response

from api_manager.models.session import Session
from api_manager.pubsub.utils import handle_pubsub_retry
from api_manager.serializers import SessionSerializer


@api_view(["GET"])
def get_session_status(request, session_id):
    session = get_object_or_404(Session, pk=session_id)
    return Response({"data": {"status": session.status}}, status=status.HTTP_200_OK)


@api_view(["POST"])
def update_session_status(request):
    data = json.loads(base64.b64decode(request.data["message"]["data"]).decode("utf-8"))

    retry_limit_exceeded = handle_pubsub_retry(request=request.data, data=data)
    if retry_limit_exceeded:
        return Response(status=status.HTTP_200_OK)

    session = get_object_or_404(Session, pk=data.pop("session_id"))
    session_serializer = SessionSerializer(
        session, data={"status": data.pop("status")}, partial=True
    )
    session_serializer.is_valid(raise_exception=True)
    session_serializer.save()
    return Response(status=status.HTTP_200_OK)
